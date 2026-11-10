import {it,expect} from 'vitest';
import {completionPayload,receiveCompletion,completionFrame,CompletionAttachment} from '../src/completion';
const payload=()=>JSON.stringify({version:1,rows:Array.from({length:37},()=>[0,0,0])});
it('completion protocol is bounded and replaces rather than ORs revocable proof',()=>{
 const s:CompletionAttachment={channel:'completion',room:'r',self:'a',nonce:'n',roster:['a','b'],expires:100000,sequence:0,at:0,payload:'',rate:[]};
 const p=JSON.parse(payload());p.rows[0]=[7,3,3];
 receiveCompletion(s,JSON.stringify({sequence:1,kind:'C',payload:JSON.stringify(p)}),1);
 expect(completionFrame(s,40001)?.ageMillis).toBe(40000);expect(completionFrame(s,45001)).toBeNull();
 receiveCompletion(s,JSON.stringify({sequence:2,kind:'C',payload:payload()}),2);expect(s.payload).toBe(payload());
 expect(()=>receiveCompletion(s,JSON.stringify({sequence:1,kind:'C',payload:payload()}),3)).toThrow();
 for(const v of [0,true,1.2,8]){p.rows[0]=[v,0,0];if(v===0)expect(completionPayload(JSON.stringify(p))).toBeDefined();else expect(()=>completionPayload(JSON.stringify(p))).toThrow();}
 p.rows.pop();expect(()=>completionPayload(JSON.stringify(p))).toThrow();
});
