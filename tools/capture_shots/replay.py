"""Deterministic local evidence replay; no ownership or server mechanics are inferred."""
from dataclasses import dataclass
from pathlib import Path
import argparse
import csv
import json
import sys

if __package__ in (None,''):
    sys.path.insert(0,str(Path(__file__).resolve().parents[2]))
from tools.capture_shots.protocol import Session,ProtocolError,plain,read_session

@dataclass(frozen=True)
class ReplayResult:
    session: Session
    summary: dict
    rows: tuple

def replay(session:Session)->ReplayResult:
    shots=[];rows=[]
    for shot in session.shots:
        shots.append({'token':shot.token,'variant':shot.input.data['variant'],'scenario':shot.input.data['scenario'],
                      'split':shot.input.data['split'],'tracks':list(shot.tracks),'association':shot.association,
                      'annotations':list(shot.annotations),'feedback_sequences':list(shot.feedback)})
        tracks=set(shot.tracks)
        rows.append(tuple(e for e in session.events if e.data.get('token')==shot.token or e.data.get('token') in tracks
                          or e.kind in ('player_state','target','gap') and shot.input.micros-500000<=e.micros<=shot.input.micros+8000000
                          or e.kind=='feedback' and e.sequence in shot.feedback
                          or e.kind=='obstruction' and (e.data['shot']==shot.token or e.data['track'] in tracks)))
    summary={'schemaVersion':1,'modelVersion':None,'prediction_available':False,'input_sha256':session.input_sha256,
             'session':session.header['metadata']['session'],'complete':session.complete,'issues':list(session.issues),
             'event_count':len(session.events),'shot_count':len(shots),'track_count':len(session.tracks),'shots':shots}
    return ReplayResult(session,summary,tuple(rows))

def output_directory(output:Path,inputs)->Path:
    output=Path(output)
    resolved=output.resolve()
    if output.exists() or output.is_symlink() or any(Path(p).resolve().is_relative_to(resolved) for p in inputs):
        raise ValueError('output must be a new directory that does not contain an input')
    resolved.mkdir(parents=True,exist_ok=False)
    return resolved

def event_row(e):
    return (e.sequence,e.micros,e.tick,e.kind,json.dumps(plain(e.data),sort_keys=True,separators=(',',':'),allow_nan=False))

def write_replay(result:ReplayResult,output:Path)->None:
    output=output_directory(output,[result.session.input_path])
    (output/'summary.json').write_text(json.dumps(result.summary,sort_keys=True,indent=2,allow_nan=False)+'\n',encoding='utf-8',newline='\n')
    # The full timeline retains unmatched evidence; per-shot views never discard the source timeline.
    for name,events in [('timeline.csv',result.session.events)]+[(f'shot-{i:04d}.csv',rows) for i,rows in enumerate(result.rows,1)]:
        with (output/name).open('w',encoding='utf-8',newline='') as f:
            writer=csv.writer(f,lineterminator='\n');writer.writerow(('sequence','micros','tick','kind','data_json'))
            writer.writerows(event_row(e) for e in events)

def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('session',type=Path);parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args(argv)
    try:
        session=read_session(args.session);write_replay(replay(session),args.output)
        print(f"Replay saved: {args.output}. Complete: {session.complete}; issues: {', '.join(session.issues) or 'none'}")
        return 0 if session.complete else 2
    except (OSError,ValueError) as e:print(f'Replay rejected: {e}',file=sys.stderr);return 1

if __name__=='__main__':raise SystemExit(main())
