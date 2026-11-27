import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { standardFiles, uploadStandardRelease } from '../PublishModrinth.mjs';

async function fixture(t, version = '5.1.0') {
  const directory = await mkdtemp(join(tmpdir(), 'sm-modrinth-test-'));
  t.after(() => rm(directory, { recursive: true, force: true }));
  const files = [];
  for (const minecraft of ['26.1.2', '26.2', '26.3']) {
    for (const edition of ['standard', 'full', 'full-no-auto-update']) {
      const suffix = edition === 'standard' ? '' : `-${edition}`;
      const name = `sparkling-mutuals-${version}${suffix}+mc${minecraft}.jar`;
      const contents = Buffer.from(name);
      files.push({ name, edition, minecraft, size: contents.length, sha256: createHash('sha256').update(contents).digest('hex') });
      await writeFile(join(directory, name), contents);
    }
  }
  await writeFile(join(directory, 'build-manifest.json'), JSON.stringify({ version, files }));
  return { directory, tag: `v${version}` };
}

test('uploads three Standard versions, with separate targets and retry-safe matching', async t => {
  const { directory, tag } = await fixture(t);
  const versions = [];
  const project = { id: 'abcdefgh', slug: 'sparklingmutuals' };
  const release = {
    tagName: tag, isDraft: false, isPrerelease: false,
    body: '# Updates\n\n### All editions\nShared\n\n### Full and Full No Auto Update\nFull line\n#### Full subsection\nHidden too\n### Standard\nStandard line\n## Downloads\nAll editions',
  };
  const request = async (url, { method, body }) => {
    assert.ok(url.startsWith('https://api.modrinth.com/v3/'));
    if (url.endsWith('/project/sparklingmutuals')) return Response.json(project);
    if (url.includes('/project/abcdefgh/version?')) return Response.json(versions);
    assert.equal(method, 'POST');
    const data = JSON.parse(body.get('data'));
    assert.equal(data.project_id, project.id);
    assert.equal(data.version_type, 'release');
    assert.equal(data.status, 'listed');
    assert.deepEqual(data.loaders, ['fabric']);
    assert.equal(data.game_versions.length, 1);
    assert.deepEqual(data.file_parts, ['jar']);
    assert.equal(data.primary_file, 'jar');
    assert.deepEqual(data.dependencies.map(item => item.project_id), ['P7dR8mSH', 'Ha28R6CL']);
    assert.equal(data.changelog, '# Updates\n\n### All editions\nShared\n\n### Standard\nStandard line');
    const jar = body.get('jar');
    assert.equal(jar.name, `sparkling-mutuals-5.1.0+mc${data.game_versions[0]}.jar`);
    const response = { ...data, files: [{ filename: jar.name, hashes: { sha512: createHash('sha512').update(Buffer.from(await jar.arrayBuffer())).digest('hex') } }] };
    versions.push(response);
    return Response.json(response);
  };
  const publish = () => uploadStandardRelease({ directory, tag, release, token: 'fake-test-token', request, log() {} });
  await publish();
  assert.deepEqual(versions.map(({ name, version_number }) => [name, version_number]), [
    ['5.1.0 for 26.1.2', '5.1.0+mc26.1.2'],
    ['5.1.0 for 26.2', '5.1.0+mc26.2'],
    ['5.1.0 for 26.3', '5.1.0+mc26.3'],
  ]);
  versions.pop();
  await publish();
  assert.equal(versions.length, 3);
  versions[0].files[0].hashes.sha512 = 'wrong';
  await assert.rejects(publish, /conflicts/);
  assert.equal(versions.length, 3);
});

test('validates the release, secret and ZIP artifact hash before any request', async t => {
  const { directory, tag } = await fixture(t, '5.1.0-beta.1');
  const release = { tagName: tag, isDraft: false, isPrerelease: true };
  const request = () => assert.fail('publisher contacted Modrinth');
  await assert.rejects(uploadStandardRelease({ directory, tag, release, request }), /MODRINTH_TOKEN/);
  await assert.rejects(uploadStandardRelease({ directory, tag, release: { ...release, isDraft: true }, token: 'fake', request }), /published GitHub release/);
  await assert.rejects(standardFiles(directory, 'v5.1.0'), /manifest does not match/);
  const files = await standardFiles(directory, tag);
  assert.equal(files[0].number, '5.1.0-beta.1+mc26.1.2');
  await writeFile(join(directory, files[0].name), 'altered');
  await assert.rejects(uploadStandardRelease({ directory, tag, release, token: 'fake', request }), /hash or size mismatch/);
});
