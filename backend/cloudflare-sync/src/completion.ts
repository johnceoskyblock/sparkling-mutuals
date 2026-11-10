import {check,object} from './protocol';

/** Version 1 uses the existing S species order (37 entries). Never infer catches from checks. */
export const COMPLETION_TTL=45000;
export type CompletionAttachment={channel:'completion';room:string;self:string;nonce:string;roster:string[];expires:number;
 sequence:number;at:number;payload:string;rate:number[]};
export function completionPayload(raw:string):string {
 check(raw,raw.length<=3072);const p=object(JSON.parse(raw));
 check(p,p.version===1&&Object.keys(p).sort().join()==='rows,version'&&Array.isArray(p.rows)&&p.rows.length===37);
 for(const row of p.rows as unknown[][])check(row,Array.isArray(row)&&row.length===3&&row.every(Number.isSafeInteger)&&
  (row[0] as number)>=0&&(row[0] as number)<=7&&(row[1] as number)>=0&&(row[1] as number)<=256&&(row[2] as number)>=0&&(row[2] as number)<=256);
 return JSON.stringify(p);
}
export function receiveCompletion(s:CompletionAttachment,raw:string,now:number){
 check(raw,raw.length<=4096&&now<s.expires);s.rate=s.rate.filter(t=>now-t<10000);check(s,s.rate.length<40);s.rate.push(now);
 const p=object(JSON.parse(raw));check(p,p.kind==='C'&&Number.isSafeInteger(p.sequence)&&(p.sequence as number)>s.sequence&&
  (p.sequence as number)<=Number.MAX_SAFE_INTEGER&&typeof p.payload==='string');
 s.payload=completionPayload(p.payload as string);s.sequence=p.sequence as number;s.at=now;
}
export function completionFrame(s:CompletionAttachment,now:number){
 return s.payload&&now-s.at<COMPLETION_TTL?{type:'state',sender:s.self,kind:'C',sequence:s.sequence,payload:s.payload,ageMillis:Math.max(0,now-s.at)}:null;
}
