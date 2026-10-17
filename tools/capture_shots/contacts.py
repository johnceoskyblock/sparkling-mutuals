"""Inspect experimental predictions separately from observed throw evidence."""
import argparse
import json
from pathlib import Path
import sys
if __package__ in (None,''):
    sys.path.insert(0,str(Path(__file__).resolve().parents[2]))
from tools.capture_shots.protocol import Session,plain,read_session
from tools.capture_shots.replay import output_directory

def summarize(session:Session)->dict:
    probes={e.data['probe']['shot']:e.data['probe'] for e in session.events if e.kind=='contact_probe'}
    shots=[]
    for shot in session.shots:
        p=probes.get(shot.token)
        feedback=[plain(e.data['feedback']) for e in session.events if e.kind=='feedback' and e.sequence in shot.feedback]
        removals=[{'track':e.data['token'],'reason':e.data['removal']} for e in session.events if e.kind=='capsule' and e.data['token'] in shot.tracks and e.data['action']=='removed']
        shots.append({'shot':shot.token,'model':p['model'] if p else None,'hypothesis':p['hypothesis'] if p else None,
            'proxy_versions':sorted({t['proxy'] for t in p['targets']}) if p else [],
            'predicted_contact':plain(p['primary']) if p else None,'coverage':plain(p['coverage']) if p else None,
            'prediction_stop':p['stop'] if p else None,'probe_incomplete':bool(p and p['truncated']),
            'raw_removals':removals,'observed_endpoint_available':False,'endpoint_error':None,
            'typed_feedback':feedback,'manual_outcomes':list(shot.annotations),'association':shot.association})
    return {'input_sha256':session.input_sha256,'schema_version':session.header['metadata']['schemaVersion'],
        'contact_prediction_available':bool(probes),'raw_file_complete':session.complete,'issues':list(session.issues),'shots':shots,
        'limitation':'Predictions are uncalibrated. Feedback, removal and absent messages do not establish endpoint coordinates.'}

def write_report(session:Session,output:Path)->None:
    output=output_directory(output,[session.input_path])
    (output/'contacts.json').write_text(json.dumps(summarize(session),sort_keys=True,indent=2,allow_nan=False)+'\n',encoding='utf-8',newline='\n')

def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('session',type=Path);parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args(argv)
    try:
        session=read_session(args.session);write_report(session,args.output);print(f'Contact diagnostics saved: {args.output}')
        return 0 if session.complete else 2
    except (OSError,ValueError) as e:print(f'Contact diagnostics rejected: {e}',file=sys.stderr);return 1
if __name__=='__main__':raise SystemExit(main())
