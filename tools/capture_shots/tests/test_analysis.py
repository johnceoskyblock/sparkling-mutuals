import copy
import csv
import json
from pathlib import Path
import tempfile
import unittest
from tools.capture_shots.analyze import analyze,read_curation,read_predictions
from tools.capture_shots.protocol import read_session
from test_protocol import records,encoded,FIXTURES

def synthetic(path,session='fit-session',variant='ordinary',split='fit',later=0,times=range(1,9),source='raw'):
    base=records();header=base[0];header['metadata']['session']=session
    input=base[1];input['data'].update(variant=variant,split=split,scenario='stationary_level')
    capsule=base[2];capsule['data']['variant']=variant
    events=[input,capsule]
    for i,t in enumerate(times):
        p={'x':2*t,'y':3*t-.5*t*t+(later if i>=2 else 0),'z':0}
        if source=='raw':
            e=copy.deepcopy(base[3]);e['data'].update(points=[{'position':p,'tickOffset':0}],resolvedPoints=[{'position':p,'tickOffset':0}],endPosition=p,resolvedEnd=p,observedMicros=int(t*1000000))
        else:
            e=copy.deepcopy(capsule);e['data'].update(action='sample',position=p)
        e.update(sequence=len(events)+1,micros=int(t*1000000),tick=int(t*20));events.append(e)
    terminal=copy.deepcopy(capsule);terminal.update(sequence=len(events)+1,micros=9000000,tick=180)
    terminal['data'].update(action='removed',removal='server_remove');events.append(terminal)
    footer=base[-1];footer.update(sequence=len(events)+1,micros=10000000,tick=200)
    footer['shots'][0].update(variant=variant,split=split,scenario='stationary_level')
    path.write_bytes(encoded([header,*events,footer]));return read_session(path)

def curate(path,*sessions):
    path.write_text(json.dumps({'schemaVersion':1,'choices':[{'sha256':s.input_sha256,'session':s.header['metadata']['session'],'shot':'s1','track':'c1','rationale':'Synthetic isolated controlled pair.'} for s in sessions]}),encoding='utf-8')
    return read_curation(path)

class AnalysisTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)
    def test_incomplete_tracks_are_excluded_unless_provisional(self):
        from dataclasses import replace
        from tools.capture_shots.protocol import Event,freeze
        for removal in ('window_expired','recording_stopped','unloaded',None):
            session=synthetic(self.root/'fit.jsonl')
            track=session.tracks[0];events=list(track.events)
            if removal is None:events.pop()
            else:
                old=events[-1];data=dict(old.data);data['removal']=removal
                events[-1]=replace(old,data=freeze(data))
            session=replace(session,tracks=(replace(track,events=tuple(events)),))
            c=curate(self.root/'c.json',session)
            result=analyze([session],[],c).summary
            self.assertEqual([],result['fitting_diagnostics'])
            self.assertEqual('incomplete_track',result['excluded'][0]['reason'])
            self.assertEqual(1,len(analyze([session],[],c,True).summary['fitting_diagnostics']))
    def test_heldout_acceleration_and_validation_seeds_are_separate(self):
        fit=synthetic(self.root/'fit.jsonl');val=synthetic(self.root/'val.jsonl','validation-session',split='validation')
        result=analyze([fit],[val],curate(self.root/'curation.json',fit,val)).summary
        self.assertAlmostEqual(-1,result['frozen_models']['ordinary']['acceleration'][1],places=9)
        self.assertEqual(4,result['fitting_diagnostics'][0]['constant_acceleration']['count'])
        self.assertEqual(6,result['validation'][0]['constant_acceleration']['count'])
        self.assertLess(result['validation'][0]['constant_acceleration']['maximum'],1e-9)
        self.assertEqual(2,result['validation'][0]['conditioning_samples'])
        self.assertIsNone(result['validation'][0]['descriptive_visual_acceleration'])
        self.assertFalse(result['gameplay_model_available'])
    def test_later_validation_changes_cannot_change_fitted_model_or_conditioning(self):
        fit=synthetic(self.root/'fit.jsonl');v1=synthetic(self.root/'v1.jsonl','val-1',split='validation')
        v2=synthetic(self.root/'v2.jsonl','val-2',split='validation',later=10)
        a=analyze([fit],[v1],curate(self.root/'c1.json',fit,v1)).summary
        b=analyze([fit],[v2],curate(self.root/'c2.json',fit,v2)).summary
        self.assertEqual(a['frozen_models'],b['frozen_models']);self.assertEqual(a['validation'][0]['seed_velocity'],b['validation'][0]['seed_velocity'])
        self.assertAlmostEqual(10,b['validation'][0]['constant_acceleration']['last_observed_error'],places=8)
    def test_renamed_same_bytes_and_edited_same_session_cannot_be_held_out(self):
        fit=synthetic(self.root/'fit.jsonl');renamed=self.root/'renamed.jsonl';renamed.write_bytes(fit.input_path.read_bytes())
        edited=synthetic(self.root/'edited.jsonl','fit-session',split='validation',later=1)
        for val in (read_session(renamed),edited):
            with self.assertRaisesRegex(ValueError,'overlap'):analyze([fit],[val],provisional=True)
    def test_masterful_without_its_own_fit_stays_unavailable(self):
        fit=synthetic(self.root/'fit.jsonl');val=synthetic(self.root/'val.jsonl','val-masterful','masterful','validation')
        result=analyze([fit],[val],curate(self.root/'c.json',fit,val)).summary
        self.assertEqual([],result['validation']);self.assertEqual('no_variant_fit',result['excluded'][-1]['reason'])
    def test_uncurated_ambiguous_incomplete_and_tick_only_are_explicit(self):
        fit=synthetic(self.root/'fit.jsonl');result=analyze([fit],[],None).summary
        self.assertEqual('uncurated',result['excluded'][0]['reason'])
        self.assertEqual(1,len(analyze([fit],[],None,True).summary['fitting_diagnostics']))
        ambiguous=read_session(FIXTURES/'ambiguous.jsonl');partial=read_session(FIXTURES/'partial.jsonl')
        result=analyze([ambiguous,partial],[]).summary
        self.assertEqual({'ambiguous','incomplete'},{e['reason'] for e in result['excluded']})
        tick=synthetic(self.root/'tick.jsonl','tick-session',source='tick')
        result=analyze([tick],[],curate(self.root/'c.json',tick)).summary
        self.assertEqual('no_raw_samples',result['excluded'][0]['reason']);self.assertEqual(8,result['tick_diagnostics'][0]['sample_count'])
    def test_curation_is_hash_bound_strict_and_input_exports_stay_unchanged(self):
        fit=synthetic(self.root/'fit.jsonl');before=fit.input_path.read_bytes()
        c=self.root/'c.json';curation=curate(c,fit);analyze([fit],[],curation)
        self.assertEqual(before,fit.input_path.read_bytes())
        d=json.loads(c.read_text());d['choices'][0]['sha256']='0'*64;c.write_text(json.dumps(d))
        with self.assertRaisesRegex(ValueError,'curation'):analyze([fit],[],read_curation(c))
        d['choices'][0]['run']='untrusted';c.write_text(json.dumps(d))
        with self.assertRaises(ValueError):read_curation(c)
    def test_external_predictions_match_only_exact_observed_times(self):
        fit=synthetic(self.root/'fit.jsonl');p=self.root/'predictions.csv'
        with p.open('w',newline='') as f:
            w=csv.writer(f);w.writerow('schemaVersion,session,shot,track,micros,x,y,z,model,coordinates'.split(','))
            w.writerow([1,'fit-session','s1','c1',1000000,2,2.5,0,'synthetic-model','session_relative_game_axes'])
        predictions=read_predictions(p)
        result=analyze([fit],[],curate(self.root/'c.json',fit),predictions=predictions).summary
        self.assertEqual(1,result['external_predictions'][0]['residuals']['count']);self.assertEqual(7,result['external_predictions'][0]['uncovered_observations'])
        self.assertEqual(0,result['external_predictions'][0]['residuals']['maximum'])
        p.write_text(p.read_text().replace('2.5','NaN'))
        with self.assertRaises(ValueError):read_predictions(p)

if __name__=='__main__':unittest.main()
