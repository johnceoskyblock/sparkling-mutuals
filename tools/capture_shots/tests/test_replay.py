from pathlib import Path
import csv
import tempfile
import unittest
import subprocess
import sys
from tools.capture_shots.protocol import read_session
from tools.capture_shots.replay import replay,write_replay
from test_protocol import FIXTURES,records,encoded

class ReplayTest(unittest.TestCase):
    def test_per_shot_view_excludes_later_unrelated_state_but_timeline_retains_it(self):
        r=records();r[-1]['sequence']=5;r[-1]['micros']=10000000;r[-1]['tick']=200
        r.insert(-1,{'type':'event','sequence':4,'micros':9000000,'tick':180,'kind':'player_state','data':{'pose':r[1]['data']['pose']}})
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp)/'input.jsonl';path.write_bytes(encoded(r));result=replay(read_session(path))
            self.assertEqual(4,len(result.session.events));self.assertEqual(3,len(result.rows[0]))
    def test_cli_exit_codes_distinguish_complete_partial_and_invalid(self):
        with tempfile.TemporaryDirectory() as temp:
            for index,(name,expected) in enumerate((('isolated.jsonl',0),('partial.jsonl',2),('invalid.jsonl',1))):
                source=FIXTURES/name if name!='invalid.jsonl' else Path(temp)/name
                if name=='invalid.jsonl':source.write_text('broken\n',encoding='utf-8')
                run=subprocess.run([sys.executable,'tools/capture_shots/replay.py',str(source),'--output',str(Path(temp)/str(index))],capture_output=True)
                self.assertEqual(expected,run.returncode,run.stderr.decode())
    def test_outputs_are_deterministic_and_keep_original_evidence_kinds(self):
        s=read_session(FIXTURES/'isolated.jsonl');r=replay(s)
        with tempfile.TemporaryDirectory() as temp:
            a=Path(temp)/'a';b=Path(temp)/'b';write_replay(r,a);write_replay(r,b)
            self.assertEqual({p.name:p.read_bytes() for p in a.iterdir()},{p.name:p.read_bytes() for p in b.iterdir()})
            with (a/'shot-0001.csv').open(newline='',encoding='utf-8') as f:rows=list(csv.DictReader(f))
            self.assertEqual(['input','capsule','packet'],[row['kind'] for row in rows])
            self.assertIn('position_sync',rows[-1]['data_json'])
            self.assertEqual(1,r.summary['shot_count']);self.assertFalse(r.summary['prediction_available'])
    def test_outputs_cannot_overwrite_input_existing_reports_or_use_aliases(self):
        with tempfile.TemporaryDirectory() as temp:
            source=Path(temp)/'input.jsonl';source.write_bytes((FIXTURES/'isolated.jsonl').read_bytes())
            before=source.read_bytes();r=replay(read_session(source))
            for output in (source,source.parent):
                with self.assertRaises(ValueError):write_replay(r,output)
            output=Path(temp)/'result';write_replay(r,output)
            with self.assertRaises(ValueError):write_replay(r,output)
            self.assertEqual(before,source.read_bytes())
    def test_ambiguous_candidates_are_retained_without_an_invented_owner(self):
        r=replay(read_session(FIXTURES/'ambiguous.jsonl'))
        self.assertEqual('ambiguous',r.summary['shots'][0]['association'])
        self.assertEqual(['c1','c2'],r.summary['shots'][0]['tracks'])

if __name__=='__main__':unittest.main()
