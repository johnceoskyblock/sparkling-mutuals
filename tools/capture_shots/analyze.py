"""Held-out visual diagnostics; no before-firing prediction or server capture model."""
from dataclasses import dataclass
from pathlib import Path
import argparse
import csv
import json
import math
import re
import statistics
import sys

if __package__ in (None,''):sys.path.insert(0,str(Path(__file__).resolve().parents[2]))
from tools.capture_shots.protocol import read_session,_pairs,_constant,MAX_BYTES
from tools.capture_shots.replay import output_directory
from tools.capture_shots.models import Vec3,Sample,VisualFit,components,fit_track,predict,residual_stats

@dataclass(frozen=True)
class AnalysisResult:
    summary:dict

@dataclass(frozen=True)
class CurationChoice:
    sha256:str
    session:str
    shot:str
    track:str
    rationale:str

@dataclass(frozen=True)
class Curation:
    choices:tuple

@dataclass(frozen=True)
class Prediction:
    session:str
    shot:str
    track:str
    micros:int
    position:Vec3
    model:str

@dataclass(frozen=True)
class PredictionSet:
    points:tuple

TOKEN=re.compile(r'[A-Za-z0-9_-]{1,48}')
COORDINATES='session_relative_game_axes'

def read_curation(path):
    path=Path(path)
    if path.stat().st_size>1024*1024:raise ValueError('curation file limit')
    d=json.loads(path.read_text(encoding='utf-8'),object_pairs_hook=_pairs,parse_constant=_constant)
    if not isinstance(d,dict) or set(d)!={'schemaVersion','choices'} or type(d['schemaVersion']) is not int or d['schemaVersion']!=1:raise ValueError('invalid curation header')
    if not isinstance(d['choices'],list) or len(d['choices'])>30000:raise ValueError('invalid curation choices')
    choices=[];shots=set();tracks=set()
    for item in d['choices']:
        if not isinstance(item,dict) or set(item)!={'sha256','session','shot','track','rationale'} or not all(isinstance(v,str) for v in item.values()):raise ValueError('invalid curation fields')
        if not re.fullmatch('[a-f0-9]{64}',item['sha256']) or any(not TOKEN.fullmatch(item[k]) for k in ('session','shot','track')):raise ValueError('invalid curation reference')
        if not 1<=len(item['rationale'].strip())<=512:raise ValueError('curation requires a short rationale')
        sk=(item['sha256'],item['session'],item['shot']);tk=(item['sha256'],item['session'],item['track'])
        if sk in shots or tk in tracks:raise ValueError('duplicate curation ownership')
        shots.add(sk);tracks.add(tk);choices.append(CurationChoice(**item))
    return Curation(tuple(choices))

def read_predictions(path):
    path=Path(path)
    if path.stat().st_size>MAX_BYTES:raise ValueError('prediction file limit')
    columns='schemaVersion,session,shot,track,micros,x,y,z,model,coordinates'.split(',')
    points=[];seen=set()
    with path.open(encoding='utf-8',newline='') as f:
        reader=csv.DictReader(f)
        if reader.fieldnames!=columns:raise ValueError('invalid prediction columns')
        for row in reader:
            if len(points)>=100000 or set(row)!=set(columns) or any(v is None for v in row.values()):raise ValueError('invalid prediction row/bound')
            if row['schemaVersion']!='1' or row['coordinates']!=COORDINATES:raise ValueError('prediction version/coordinates mismatch')
            if any(not TOKEN.fullmatch(row[k]) for k in ('session','shot','track')) or not re.fullmatch(r'[A-Za-z0-9_.-]{1,64}',row['model']):raise ValueError('invalid prediction identifier')
            if not re.fullmatch(r'[0-9]{1,19}',row['micros']):raise ValueError('invalid prediction time')
            p=Vec3(*(float(row[k]) for k in ('x','y','z')))
            if not all(math.isfinite(v) for v in components(p)):raise ValueError('nonfinite prediction')
            key=tuple(row[k] for k in ('session','shot','track','micros','model'))
            if key in seen:raise ValueError('duplicate prediction key')
            seen.add(key);points.append(Prediction(row['session'],row['shot'],row['track'],int(row['micros']),p,row['model']))
    return PredictionSet(tuple(points))

def _position(d):return Vec3(d['x'],d['y'],d['z'])

def samples(track,source='raw'):
    # Stepped server offsets stay in replay. Default diagnostics use one endpoint at each observed arrival,
    # avoiding an invented authoritative server launch/tick schedule.
    result={};problems=[];coincident=0
    for event in track.events:
        d=event.data;position=None;micros=event.micros
        if source=='tick' and event.kind=='capsule' and d['action']=='sample':position=d['position']
        if source=='raw' and event.kind=='packet' and d['update'] in ('spawn','move','position_sync','teleport'):
            if d['truncated']:problems.append('truncated_path');continue
            if not d['baseKnown']:problems.append('missing_base');continue
            position=d['resolvedEnd']
            if position is None and not d['relative'] and d['points']:position=d['points'][-1]['position']
            if position is None:
                if d['points']:problems.append('missing_resolution')
                continue
            micros=d['observedMicros'] if d['observedMicros'] is not None else event.micros
        if position is not None:
            if micros in result:coincident+=1
            result[micros]=Sample(micros/1000000,_position(position),source)
    return tuple(result[t] for t in sorted(result)),tuple(sorted(set(problems))),coincident

def _error(fit,observed):
    return [math.dist(components(predict(fit,s.seconds)),components(s.position)) for s in observed]

def _motion(observed,allow_fit=True):
    intervals=[b.seconds-a.seconds for a,b in zip(observed,observed[1:])]
    speeds=[math.dist(components(a.position),components(b.position))/dt for a,b,dt in zip(observed,observed[1:],intervals)]
    fit=fit_track(observed,2) if allow_fit else None
    return {'sample_count':len(observed),'mean_interval_seconds':statistics.mean(intervals) if intervals else None,
            'maximum_gap_seconds':max(intervals) if intervals else None,'mean_visual_speed_blocks_per_second':statistics.mean(speeds) if speeds else None,
            'descriptive_visual_acceleration':list(components(fit.acceleration)) if fit else None}

def _seed(observed,acceleration):
    first,second=observed[:2];dt=second.seconds-first.seconds
    velocity=Vec3(*((b-a-.5*g*dt*dt)/dt for a,b,g in zip(components(first.position),components(second.position),components(acceleration))))
    return VisualFit(first.seconds,first.position,velocity,acceleration,first.source)

def analyze(fitting,validation,curation=None,provisional=False,predictions=None):
    fitting=tuple(fitting);validation=tuple(validation)
    fit_tokens={s.header['metadata']['session'] for s in fitting};fit_hashes={s.input_sha256 for s in fitting}
    if any(s.header['metadata']['session'] in fit_tokens or s.input_sha256 in fit_hashes for s in validation):raise ValueError('fit/validation overlap by session or source hash')
    for group in (fitting,validation):
        if len({s.input_sha256 for s in group})!=len(group) or len({s.header['metadata']['session'] for s in group})!=len(group):raise ValueError('duplicate session in one split')
    all_sessions=fitting+validation
    if any(s.header['metadata']['coordinates']!=COORDINATES for s in all_sessions):raise ValueError('mixed coordinate bases')
    choices={}
    for c in curation.choices if curation else ():
        match=next((s for s in all_sessions if s.input_sha256==c.sha256 and s.header['metadata']['session']==c.session),None)
        if match is None or c.shot not in {s.token for s in match.shots} or c.track not in {t.token for t in match.tracks}:raise ValueError('curation reference/hash is not in these inputs')
        choices[(c.sha256,c.shot)]=c.track
    result={'schemaVersion':1,'kind':'diagnostic_visual_baselines','gameplay_model_available':False,
            'provisional':provisional,'sampling_basis':'raw_arrival_endpoints','coordinates':COORDINATES,
            'frozen_models':{},'fitting_diagnostics':[],'validation':[],'excluded':[],'tick_diagnostics':[],
            'external_predictions':[],'sources':[{'session':s.header['metadata']['session'],'sha256':s.input_sha256,'complete':s.complete} for s in all_sessions]}
    def exclude(s,shot,role,reason,track=None):
        result['excluded'].append({'session':s.header['metadata']['session'],'shot':shot.token,'variant':shot.input.data['variant'],'role':role,'track':track,'reason':reason})
    def collect(group,role):
        collected=[]
        for s in group:
            tracks={t.token:t for t in s.tracks}
            for shot in s.shots:
                selected=choices.get((s.input_sha256,shot.token))
                reason='incomplete' if not s.complete else 'ambiguous' if shot.association=='ambiguous' else 'unmatched' if shot.association=='unmatched' else 'uncurated' if selected is None else None
                if reason and not provisional:exclude(s,shot,role,reason);continue
                if shot.input.data['split']!=role and not provisional:exclude(s,shot,role,'split_label_mismatch');continue
                tokens=(selected,) if selected else shot.tracks
                if not tokens:exclude(s,shot,role,'unmatched');continue
                for token in tokens:
                    track=tracks.get(token)
                    if track is None or track.variant!=shot.input.data['variant']:exclude(s,shot,role,'variant_or_track_mismatch',token);continue
                    terminal=next((e for e in reversed(track.events) if e.kind=='capsule' and e.data['action']=='removed'),None)
                    if (terminal is None or terminal.data['removal']!='server_remove') and not provisional:
                        exclude(s,shot,role,'incomplete_track',token);continue
                    observed,problems,coincident=samples(track)
                    tick,_,_=samples(track,'tick')
                    key={'session':s.header['metadata']['session'],'shot':shot.token,'track':token,'variant':track.variant,'role':role,'association':shot.association}
                    if tick:result['tick_diagnostics'].append({**key,**_motion(tick,role=='fit'),'sampling_basis':'tick_only_not_pooled'})
                    if problems and not provisional:exclude(s,shot,role,problems[0],token);continue
                    if len(observed)<3:exclude(s,shot,role,'no_raw_samples' if not observed else 'insufficient_samples',token);continue
                    if shot.input.micros>int(observed[0].seconds*1000000) and not provisional:exclude(s,shot,role,'preexisting_track',token);continue
                    feedback=[e for e in s.events if e.kind=='feedback' and e.sequence in shot.feedback]
                    metrics={**key,**_motion(observed,role=='fit'),'coincident_updates':coincident,'observation_problems':list(problems),
                             'input_to_first_observation_seconds':observed[0].seconds-shot.input.micros/1000000,
                             'input_to_feedback_seconds':[e.micros/1000000-shot.input.micros/1000000 for e in feedback],
                             'provisional':provisional,'curated':selected is not None}
                    collected.append((s,shot,track,observed,metrics))
        return collected
    fit_tracks=collect(fitting,'fit');validation_tracks=collect(validation,'validation')
    accelerations={}
    for s,shot,track,observed,metrics in fit_tracks:
        half=len(observed)//2
        cv=fit_track(observed[:half],1);ca=fit_track(observed[:half],2)
        if cv is None or ca is None:exclude(s,shot,'fit','insufficient_or_rank_deficient_fit',track.token);continue
        accelerations.setdefault(track.variant,[]).append(ca.acceleration)
        result['fitting_diagnostics'].append({**metrics,'training_samples':half,'held_out_samples':len(observed)-half,
            'constant_velocity':residual_stats(_error(cv,observed[half:])),'constant_acceleration':residual_stats(_error(ca,observed[half:]))})
    frozen={}
    for variant,values in sorted(accelerations.items()):
        acceleration=Vec3(*(statistics.median(components(v)[axis] for v in values) for axis in range(3)))
        frozen[variant]=acceleration
        result['frozen_models'][variant]={'acceleration':list(components(acceleration)),'fitting_track_count':len(values),'constant_velocity_acceleration':[0,0,0]}
    for s,shot,track,observed,metrics in validation_tracks:
        if track.variant not in frozen:exclude(s,shot,'validation','no_variant_fit',track.token);continue
        ca=_seed(observed,frozen[track.variant]);cv=_seed(observed,Vec3(0,0,0))
        result['validation'].append({**metrics,'conditioning_samples':2,'seed_velocity':list(components(ca.velocity)),
            'constant_velocity':residual_stats(_error(cv,observed[2:])),'constant_acceleration':residual_stats(_error(ca,observed[2:]))})
    if predictions:
        matched=set()
        for s,shot,track,observed,metrics in fit_tracks+validation_tracks:
            matching=[(i,p) for i,p in enumerate(predictions.points) if (p.session,p.shot,p.track)==(s.header['metadata']['session'],shot.token,track.token)]
            for model in sorted({p.model for _,p in matching}):
                by_time={p.micros:(i,p) for i,p in matching if p.model==model};errors=[]
                for sample in observed:
                    item=by_time.get(round(sample.seconds*1000000))
                    if item:
                        i,p=item;matched.add(i);errors.append(math.dist(components(sample.position),components(p.position)))
                result['external_predictions'].append({**{k:metrics[k] for k in ('session','shot','track','variant','role')},'model':model,
                    'residuals':residual_stats(errors),'uncovered_observations':len(observed)-len(errors)})
        result['unmatched_prediction_rows']=len(predictions.points)-len(matched)
    result['counts']={role:{'input_shots':sum(len(s.shots) for s in group),'scored_tracks':len(result['fitting_diagnostics' if role=='fit' else 'validation']),
                           'excluded':sum(e['role']==role for e in result['excluded'])} for role,group in (('fit',fitting),('validation',validation))}
    return AnalysisResult(result)

def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fit',action='append',type=Path,default=[]);parser.add_argument('--validation',action='append',type=Path,default=[])
    parser.add_argument('--curation',type=Path);parser.add_argument('--provisional',action='store_true');parser.add_argument('--predictions',type=Path);parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args(argv)
    try:
        fitting=[read_session(p) for p in args.fit];validation=[read_session(p) for p in args.validation]
        result=analyze(fitting,validation,read_curation(args.curation) if args.curation else None,args.provisional,read_predictions(args.predictions) if args.predictions else None)
        output=output_directory(args.output,args.fit+args.validation+[p for p in (args.curation,args.predictions) if p])
        (output/'analysis.json').write_text(json.dumps(result.summary,sort_keys=True,indent=2,allow_nan=False)+'\n',encoding='utf-8',newline='\n')
        print(f'Visual diagnostics saved: {output}. No gameplay model is selected.');return 0
    except (OSError,ValueError) as e:print(f'Analysis rejected: {e}',file=sys.stderr);return 1

if __name__=='__main__':raise SystemExit(main())
