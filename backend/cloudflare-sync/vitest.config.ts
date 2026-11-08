import { defineConfig } from 'vitest/config';
import {cloudflareTest} from '@cloudflare/vitest-plugin';
export default defineConfig({plugins:[cloudflareTest({wrangler:{configPath:'./wrangler.jsonc'},miniflare:{bindings:{SIGNING_SECRET:'test-only-secret-not-for-deployment-123456'}}})],test:{include:['test/**/*.test.ts']}});
