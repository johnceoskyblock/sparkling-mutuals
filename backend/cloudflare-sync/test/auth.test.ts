import {it,expect} from 'vitest';
import {issueToken,verifyToken} from '../src/auth';
const id='0'.repeat(32),secret='test-only-secret-not-for-deployment-123456';
it('signs independent origin-scoped short leases and rejects tampering, other origins, expiry and weak keys',async()=>{
 const token=await issueToken(id,secret,'https://backup.example',1000);
 expect(await verifyToken(token,secret,'https://backup.example',1001)).toBe(id);
 for(const [t,s,o,n] of [[token+'x',secret,'https://backup.example',1001],[token,secret,'https://other.example',1001],[token,secret,'https://backup.example',601000],[token,'wrong','https://backup.example',1001]] as const)
  await expect(verifyToken(t,s,o,n)).rejects.toThrow();
 await expect(issueToken(id,'weak','https://backup.example',0)).rejects.toThrow();
});
