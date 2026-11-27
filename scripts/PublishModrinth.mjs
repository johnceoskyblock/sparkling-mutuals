import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const api = 'https://api.modrinth.com/v3';
const repository = 'johnceoskyblock/sparkling-mutuals';
const slug = 'sparklingmutuals';
const dependencies = ['P7dR8mSH', 'Ha28R6CL'].map(project_id => ({ project_id, dependency_type: 'required' }));

export function standardChangelog(body) {
  const output = [];
  let excludedLevel = 0;
  for (const line of (body ?? '').split(/\r?\n/)) {
    const heading = /^(#{1,6})\s+(.+)$/.exec(line);
    if (heading) {
      const level = heading[1].length;
      const label = heading[2].replace(/[*`]/g, '').trim();
      if (excludedLevel && level <= excludedLevel) excludedLevel = 0;
      if (!excludedLevel && /^(?:full\b|upcoming full\b|downloads$)/i.test(label)) excludedLevel = level;
    }
    if (!excludedLevel) output.push(line);
  }
  return output.join('\n').trim();
}

const hash = (algorithm, contents) => createHash(algorithm).update(contents).digest('hex');

export async function standardFiles(directory, tag) {
  const match = /^v(\d+\.\d+\.\d+(?:-beta\.\d+)?)$/.exec(tag ?? '');
  if (!match) throw new Error('Expected a stable or beta release tag.');
  const version = match[1];
  const manifest = JSON.parse(await readFile(join(directory, 'build-manifest.json'), 'utf8'));
  if (manifest.version !== version || !Array.isArray(manifest.files)) {
    throw new Error('The verified manifest does not match the release tag.');
  }
  const targets = new Set(manifest.files.map(file => file.minecraft));
  const standard = manifest.files.filter(file => file.edition === 'standard');
  if (targets.size === 0 || standard.length !== targets.size || new Set(standard.map(file => file.minecraft)).size !== targets.size) {
    throw new Error('Expected one verified Standard JAR per Minecraft target.');
  }
  return Promise.all(standard.map(async file => {
    if (!/^\d+(?:\.\d+)+$/.test(file.minecraft) || file.name !== `sparkling-mutuals-${version}+mc${file.minecraft}.jar`) {
      throw new Error('Unexpected Standard JAR in the verified manifest.');
    }
    const contents = await readFile(join(directory, file.name));
    if (file.size !== contents.length || file.sha256 !== hash('sha256', contents)) {
      throw new Error(`Verified manifest hash or size mismatch for ${file.name}.`);
    }
    return { ...file, contents, sha512: hash('sha512', contents), version, number: `${version}+mc${file.minecraft}` };
  }));
}

export async function uploadStandardRelease({ directory, tag, release, token, request = fetch, log = console.log }) {
  const beta = tag?.includes('-beta.') === true;
  if (release.tagName !== tag || release.isDraft !== false || release.isPrerelease !== beta) {
    throw new Error('Expected the published GitHub release for this tag.');
  }
  if (!token?.trim()) throw new Error('Set the MODRINTH_TOKEN GitHub Actions secret before publishing.');
  const files = await standardFiles(directory, tag);
  const headers = { Authorization: token.trim(), 'User-Agent': `${repository}/release-workflow` };
  async function call(path, body) {
    const response = await request(api + path, {
      method: body ? 'POST' : 'GET', body, headers, redirect: 'error', signal: AbortSignal.timeout(120_000),
    });
    if (!response.ok) throw new Error(`Modrinth ${body ? 'upload' : 'lookup'} failed for ${path} (HTTP ${response.status}).`);
    return response.json();
  }
  const project = await call(`/project/${slug}`);
  if (project.slug !== slug || !/^[a-zA-Z0-9]{8}$/.test(project.id)) {
    throw new Error('Modrinth lookup did not return the Sparkling Mutuals project.');
  }
  const existing = await call(`/project/${project.id}/version?include_changelog=false`);
  const channel = beta ? 'beta' : 'release';
  function matches(file, entry) {
    const remote = entry.files?.[0];
    return entry.version_type === channel && entry.game_versions?.length === 1 && entry.game_versions[0] === file.minecraft &&
      entry.loaders?.length === 1 && entry.loaders[0] === 'fabric' && entry.files?.length === 1 &&
      remote?.filename === file.name && remote?.hashes?.sha512 === file.sha512;
  }
  const pending = files.filter(file => {
    const candidates = existing.filter(entry => entry.version_number === file.number ||
      ([file.version, tag].includes(entry.version_number) && entry.game_versions?.includes(file.minecraft)) ||
      entry.files?.some(remote => remote.filename === file.name));
    if (candidates.length === 0) return true;
    if (candidates.length !== 1 || !matches(file, candidates[0])) {
      throw new Error(`An existing Modrinth version conflicts with ${file.number}. Review it before rerunning.`);
    }
    log(`Already uploaded ${file.number}.`);
    return false;
  });
  for (const file of pending) {
    const form = new FormData();
    form.set('data', JSON.stringify({
      project_id: project.id,
      name: `${file.version} for ${file.minecraft}`,
      version_number: file.number,
      changelog: standardChangelog(release.body),
      version_type: channel,
      loaders: ['fabric'],
      game_versions: [file.minecraft],
      environment: 'client_only',
      status: 'listed',
      featured: false,
      dependencies,
      file_parts: ['jar'],
      primary_file: 'jar',
    }));
    form.set('jar', new Blob([file.contents], { type: 'application/java-archive' }), file.name);
    const uploaded = await call('/version', form);
    if (uploaded.project_id !== project.id || uploaded.version_number !== file.number || !matches(file, uploaded)) {
      throw new Error(`Modrinth did not confirm the expected version for ${file.number}. Review it before rerunning.`);
    }
    log(`Uploaded Modrinth version ${file.number}.`);
  }
}

async function main() {
  const [directory, releasePath] = process.argv.slice(2);
  const { CI, GITHUB_EVENT_NAME, GITHUB_REF, GITHUB_REF_NAME, GITHUB_REPOSITORY, MODRINTH_TOKEN } = process.env;
  if (CI !== 'true' || GITHUB_EVENT_NAME !== 'push' || GITHUB_REPOSITORY !== repository ||
      !GITHUB_REF?.startsWith('refs/tags/v') || GITHUB_REF !== `refs/tags/${GITHUB_REF_NAME}`) {
    throw new Error('Modrinth publishing requires a tag push in the main repository.');
  }
  if (!directory || !releasePath) throw new Error('Expected the verified asset directory and GitHub release JSON.');
  const release = JSON.parse(await readFile(releasePath, 'utf8'));
  await uploadStandardRelease({ directory, tag: GITHUB_REF_NAME, release, token: MODRINTH_TOKEN });
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  main().catch(error => { console.error(error.message); process.exitCode = 1; });
}
