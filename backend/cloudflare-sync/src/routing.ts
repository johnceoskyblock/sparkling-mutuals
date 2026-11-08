import {check,identity,integer,object,text} from './protocol';
export interface RouteRequest {version:1;lobby:string;self:string;roster:string[];nonce:string;unusableMs:number;action:'check'|'leave'}
export interface Registration {nonce:string;renewed:number;retired:boolean;completed:boolean}
export interface RouteRoom {generation:string;lobby:string;roster:string[];provider:'primary'|'backup';members:Record<string,Registration>;cohort:string[];renewed:number}
export interface RouteResult {version:1;room:RouteRoom;generation:string;provider:'primary'|'backup';admitted:boolean;nonce:string}
export function request(input:unknown):RouteRequest {
 const o=object(input);integer(o.version,1,1);const id=identity(o.lobby,o.self,o.roster),nonce=text(o.nonce,64);
 check(nonce,/^[a-zA-Z0-9_-]{16,64}$/.test(nonce));check(o.action,o.action==='check'||o.action==='leave');
 return {...id,version:1,nonce,unusableMs:integer(o.unusableMs,0,86400000),action:o.action as RouteRequest['action']};
}
export function route(previous:RouteRoom|undefined,raw:RouteRequest,now:number):RouteResult {
 const q=request(raw);let r=previous;
 const expired=r&&now-r.renewed>=600000;
 const allLeft=r&&Object.values(r.members).length>0&&Object.values(r.members).every(m=>m.retired);
 // A stale departure must never create/replace a generation or retire a different nonce.
 if(q.action==='leave'){
  check(r,!!r);const member=r!.members[q.self];if(member?.nonce===q.nonce)member.retired=true;
  return {version:1,room:r!,generation:r!.generation,provider:r!.provider,admitted:false,nonce:q.nonce};
 }
 if(r&&allLeft&&!expired&&r.members[q.self]?.nonce===q.nonce)return {version:1,room:r,generation:r.generation,provider:r.provider,admitted:false,nonce:q.nonce};
 if(!r||expired||allLeft){r={generation:crypto.randomUUID(),lobby:q.lobby,roster:q.roster,provider:'primary',members:{},cohort:[],renewed:now};}
 check(r,r.lobby===q.lobby&&r.roster.every(id=>q.roster.includes(id)));
 if(r.roster.length!==q.roster.length){
  // Only a never-registered new member can confirm an expansion; old subsets cannot roll it back.
  check(q,!r.roster.includes(q.self)&&!r.members[q.self]);r.roster=q.roster;
 }
 let m=r.members[q.self];
 if(m&&m.nonce!==q.nonce){m.retired=true;return {version:1,room:r,generation:r.generation,provider:r.provider,admitted:false,nonce:q.nonce};}
 if(m?.retired)return {version:1,room:r,generation:r.generation,provider:r.provider,admitted:false,nonce:q.nonce};
 m??=r.members[q.self]={nonce:q.nonce,renewed:now,retired:false,completed:false};m.renewed=now;r.renewed=now;
 if(r.provider==='primary'&&q.unusableMs>=30000){r.provider='backup';r.cohort=Object.keys(r.members).filter(id=>!r!.members[id].retired&&now-r!.members[id].renewed<90000);}
 return {version:1,room:r,generation:r.generation,provider:r.provider,admitted:true,nonce:q.nonce};
}
export function reconciled(r:RouteRoom):boolean{return r.cohort.every(id=>r.members[id]?.retired||r.members[id]?.completed);}
