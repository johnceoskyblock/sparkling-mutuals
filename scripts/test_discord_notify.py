"""Execute the actual inline workflow sender against controlled external boundaries."""
import contextlib
import copy
import hashlib
import hmac
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

REPOSITORY = 'johnceoskyblock/sparkling-mutuals'
SECRET = 'a' * 64
READ_TOKEN = 'ghs_12345_' + ('a-_' * 60) + '.' + ('b' * 180) + '.' + ('c' * 180)
STAMP = '1700000000'
WORKFLOW = Path(__file__).resolve().parents[1] / '.github/workflows/discord-notify.yml'


def event():
    return {'repository': {'full_name': REPOSITORY}, 'workflow_run': {
        'id': 42, 'name': 'build', 'path': '.github/workflows/build.yml', 'event': 'push',
        'status': 'completed', 'conclusion': 'success', 'head_branch': 'master',
        'repository': {'full_name': REPOSITORY, 'id': 100},
        'head_repository': {'full_name': REPOSITORY, 'id': 100}}}


class SenderTest(unittest.TestCase):
    def execute(self, data=None, extra_env=None, network_error=None):
        self.assertTrue(WORKFLOW.exists(), 'the signed notification workflow must exist')
        text = WORKFLOW.read_text(encoding='utf-8')
        marker = '        run: |\n'
        self.assertIn(marker, text)
        script = '\n'.join(line[10:] for line in text.split(marker, 1)[1].splitlines() if line.startswith('          '))
        requests, handlers = [], []

        class Opener:
            def open(self, request, timeout):
                requests.append((request, timeout))
                if network_error:
                    raise network_error
                return io.BytesIO(b'{"status":"sent"}')

        def opener(*values):
            handlers.extend(values)
            return Opener()

        with tempfile.NamedTemporaryFile(mode='w', encoding='utf-8', delete=False) as stream:
            json.dump(event() if data is None else data, stream)
            event_file = stream.name
        output = io.StringIO(); code = 0
        environment = {'GITHUB_EVENT_PATH': event_file, 'GITHUB_EVENT_NAME': 'workflow_run',
            'GITHUB_REPOSITORY': REPOSITORY, 'GITHUB_REF': 'refs/heads/master',
            'SM_NOTIFICATION_URL': 'https://example.test/notifications', 'SM_NOTIFICATION_SECRET': SECRET,
            'SM_GITHUB_READ_TOKEN': READ_TOKEN}
        environment.update(extra_env or {})
        try:
            with patch.dict(os.environ, environment), patch('time.time', return_value=int(STAMP)), \
                    patch('urllib.request.build_opener', side_effect=opener), \
                    contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
                try:
                    exec(compile(script, str(WORKFLOW), 'exec'), {'__name__': '__main__'})
                except SystemExit as error:
                    code = error.code or 0
        finally:
            os.unlink(event_file)
        self.assertNotIn(SECRET, output.getvalue())
        self.assertNotIn(READ_TOKEN, output.getvalue())
        return code, requests, handlers, output.getvalue()

    def test_master_push_sends_exact_signed_payload_without_redirects(self):
        code, requests, handlers, _ = self.execute()
        self.assertEqual(code, 0)
        self.assertEqual(len(requests), 1)
        request, timeout = requests[0]
        body = json.dumps({'repository': REPOSITORY, 'kind': 'action', 'runId': '42', 'githubReadToken': READ_TOKEN}, separators=(',', ':')).encode()
        self.assertEqual(request.data, body)
        self.assertEqual(request.get_method(), 'POST')
        self.assertEqual(request.full_url, 'https://example.test/notifications')
        headers = {key.lower(): value for key, value in request.header_items()}
        self.assertEqual(headers['x-sm-timestamp'], STAMP)
        self.assertEqual(headers['user-agent'], 'Sparkling-Mutuals-Notifications')
        self.assertEqual(headers['x-sm-signature'], 'sha256=' + hmac.new(SECRET.encode(), STAMP.encode() + b'.' + body, hashlib.sha256).hexdigest())
        self.assertEqual(timeout, 45)
        self.assertTrue(any(handler.redirect_request(request, None, 302, '', {}, 'https://other.test') is None for handler in handlers))

    def test_verified_release_and_manual_test_use_the_correct_kind(self):
        release = event(); release['workflow_run'].update(name='release', path='.github/workflows/release.yml', head_branch='v3.3.0-beta.1')
        code, requests, _, _ = self.execute(release)
        self.assertEqual(code, 0); self.assertEqual(json.loads(requests[0][0].data)['kind'], 'release')
        manual = {'repository': {'full_name': REPOSITORY}, 'inputs': {'kind': 'action', 'run_id': '123'}}
        code, requests, _, _ = self.execute(manual, {'GITHUB_EVENT_NAME': 'workflow_dispatch'})
        self.assertEqual(code, 0); self.assertEqual(json.loads(requests[0][0].data)['runId'], '123')

    def test_failed_pr_fork_unknown_workflow_and_other_branch_make_no_request(self):
        changes = [{'conclusion': 'failure'}, {'event': 'pull_request'}, {'head_branch': 'feature'},
            {'path': '.github/workflows/other.yml'}, {'head_repository': {'full_name': 'someone/fork', 'id': 101}}]
        for change in changes:
            with self.subTest(change=change):
                data = copy.deepcopy(event()); data['workflow_run'].update(change)
                code, requests, _, _ = self.execute(data)
                self.assertEqual(code, 0); self.assertEqual(requests, [])

    def test_bad_configuration_and_network_failures_do_not_leak_secrets_or_response_bodies(self):
        for destination in ['http://example.test/notifications', 'https://example.test/notifications?key=private', 'https://user:password@example.test/notifications']:
            code, requests, _, _ = self.execute(extra_env={'SM_NOTIFICATION_URL': destination})
            self.assertNotEqual(code, 0); self.assertEqual(requests, [])
        code, _, _, output = self.execute(network_error=RuntimeError('private upstream detail ' + SECRET))
        self.assertNotEqual(code, 0); self.assertNotIn('private upstream detail', output)

    def test_sender_requires_temporary_read_token_and_only_read_permissions(self):
        for token in ['', 'not-an-actions-token', 'ghs_' + 'b' * 36 + '\n', 'ghs_' + 'b' * 4093]:
            code, requests, _, _ = self.execute(extra_env={'SM_GITHUB_READ_TOKEN': token})
            self.assertNotEqual(code, 0); self.assertEqual(requests, [])
        text = WORKFLOW.read_text(encoding='utf-8')
        self.assertIn('  actions: read', text)
        self.assertIn('SM_GITHUB_READ_TOKEN: ${{ github.token }}', text)

    def test_legacy_token_remains_accepted_and_current_token_is_treated_as_opaque(self):
        for token in ['ghs_' + 'b' * 36, READ_TOKEN]:
            code, requests, _, output = self.execute(extra_env={'SM_GITHUB_READ_TOKEN': token})
            self.assertEqual(code, 0)
            self.assertEqual(json.loads(requests[0][0].data)['githubReadToken'], token)
            self.assertNotIn(token, output)


if __name__ == '__main__':
    unittest.main()
