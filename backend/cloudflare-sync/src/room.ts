import {Allocation,Fairness} from './allocator';
import {RouteRoom,reconciled} from './routing';
import {Biome,CODES,check,integer,locks,numbers,object,text,uuid} from './protocol';
export interface Evidence {reporter:string;source:string;nests:number;facts:number[];caught:number[]}
export interface Snapshot {sequence:number;kind:string;payload:string;catchUpOnly?:boolean}
export interface Attachment {
 room:string;self:string;nonce:string;roster:string[];expires:number;rate:number[];sequence:number;
 snapshots:Record<string,Snapshot>;union:number[];evidence:Evidence[];
 bootstrap:{stage:'none'|'open'|'done';facts:Evidence[];locks:Record<string,Biome>;instructed:Record<string,Biome>;fairness:Fairness|null};
}
export const FACT_INDICES=[23,24,25,26,27,28,29,30,31,32,34,35,36,37];
export function attachment(room:string,self:string,nonce:string,roster:string[],now:number):Attachment{
 return {room,self,nonce,roster,expires:now+600000,rate:[],sequence:0,snapshots:{},union:[0,0,0,0],evidence:[],bootstrap:{stage:'none',facts:[],locks:{},instructed:{},fairness:null}};
}
export function evidence(input:unknown,roster:string[]):Evidence{
 const v=object(input),reporter=uuid(v.reporter),source=v.source??'reporter';check(v,roster.includes(reporter)&&(source==='reporter'||source==='room'));const nests=integer(v.nests,0,8191);
 check(v,Array.isArray(v.facts)&&v.facts.length===38);const facts=v.facts as number[];
 // Bootstrap may contain irreversible facts only, never inventory/feeder state or positive sightings.
 const probe=[...facts];probe[17]=-1;probe[33]=2;numbers('A',probe.join(','));
 facts.forEach((n,i)=>{integer(n);check(n,FACT_INDICES.includes(i)||n===0);});
 check(v,Array.isArray(v.caught)&&v.caught.length===2);const caught=[integer(v.caught[0],0,1073741823),integer(v.caught[1],0,127)];
 return {reporter,source:source as string,nests,facts:[...facts],caught};
}
export class Room {
 constructor(public route:RouteRoom,public allocation:Allocation,public sessions:Attachment[]){}
 get ready(){return reconciled(this.route)&&!this.allocation.conflict;}
 union(){const result=[0,0,0,0];for(const s of this.sessions)for(let i=0;i<4;i++)result[i]|=s.union[i];return result;}
 private mergeUnion(values:number[]){for(const s of this.sessions)for(let i=0;i<4;i++)s.union[i]|=values[i];}
 receive(s:Attachment,raw:string,now:number):{broadcast:unknown[];private:unknown[];leave:boolean}{
  check(raw,new TextEncoder().encode(raw).length<=4096&&now<s.expires);s.rate=s.rate.filter(t=>now-t<10000);check(raw,s.rate.length<40);s.rate.push(now);
  const v=object(JSON.parse(raw)),sequence=integer(v.sequence,1);check(v,sequence>s.sequence);s.sequence=sequence;
  const kind=text(v.kind,1),payload=text(v.payload,3072);check(v,v.sender===undefined);
  if(v.catchUpOnly!==undefined)check(v,kind==='A'&&typeof v.catchUpOnly==='boolean');
  const broadcast:unknown[]=[],privateFrames:unknown[]=[];let leave=false;
  if(kind==='F'){
   const f=object(JSON.parse(payload));integer(f.version,1,1);const action=text(f.action);
   if(action==='begin'){check(f,s.bootstrap.stage==='none');s.bootstrap.stage='open';}
   else {check(f,s.bootstrap.stage==='open');
    if(action==='facts'){check(f,s.bootstrap.facts.length<8);const e=evidence(f,this.route.roster);check(e,!s.bootstrap.facts.some(old=>old.reporter===e.reporter&&old.source===e.source));s.bootstrap.facts.push(e);}
    else if(action==='locks'){s.bootstrap.locks=locks(f.locked,this.route.roster);s.bootstrap.instructed=locks(f.instructed,this.route.roster);}
    else if(action==='fairness'){const o=object(f.fairness),counts=object(o.counts),clean:Record<string,number>={};for(const [id,n]of Object.entries(counts)){check(id,this.route.roster.includes(uuid(id)));clean[id]=integer(n,0,1000000);}s.bootstrap.fairness={run:text(o.run),revision:integer(o.revision),counts:clean};}
    else if(action==='done'){
     this.allocation.importLocks(s.bootstrap.locks);this.allocation.importLocks(s.bootstrap.instructed);Object.assign(this.allocation.instructed,s.bootstrap.instructed);
     if(s.bootstrap.fairness)this.allocation.seed(s.bootstrap.fairness);
     s.bootstrap.stage='done';const member=this.route.members[s.self];check(member,!!member&&member.nonce===s.nonce&&!member.retired);member.completed=true;
     for(const e of s.bootstrap.facts){this.mergeUnion([0,0,...e.caught]);for(const other of this.sessions){const old=other.evidence.find(v=>v.reporter===e.reporter&&v.source===e.source);if(old){old.nests|=e.nests;FACT_INDICES.forEach(i=>old.facts[i]=i===24?Math.max(old.facts[i],e.facts[i]):old.facts[i]|e.facts[i]);old.caught=old.caught.map((v,i)=>v|e.caught[i]);}else other.evidence.push(structuredClone(e));check(other,other.evidence.length<=8);}broadcast.push({type:'bootstrap-facts',payload:e});}
    }else check(action,false);
   }
  } else {
   check(s,s.bootstrap.stage==='done');
   if(kind==='H'){
    const h=object(JSON.parse(payload));integer(h.version,1,1);
    switch(h.action){
     case 'hello':this.allocation.hello(s.self,h,now);break;
     case 'report':{const player=uuid(h.player);check(h,this.route.roster.includes(player));const b=text(h.biome) as Biome;this.allocation.report(player,b,now);break;}
     case 'ack':this.allocation.ack(s.self,text(h.id,200));break;
     case 'leave':this.route.members[s.self].retired=true;this.allocation.leave(s.self);leave=true;break;
     default:check(h,false);
    }
   } else {
    if(kind==='B')check(payload,CODES.includes(payload));else numbers(kind,payload);
    const snap={sequence,kind,payload,...(v.catchUpOnly?{catchUpOnly:true}: {})};s.snapshots[kind==='B'?'B'+payload:kind]=snap;
    if(kind==='S')this.mergeUnion(numbers(kind,payload));
    if(!v.catchUpOnly&&(kind!=='S'||this.ready))broadcast.push({type:'state',sender:s.self,sequence,kind,payload});
   }
  }
  this.allocation.allocate(now,this.ready);
  // Atomic room union always precedes S relay and may only exist after the fixed cohort reconciles.
  if(this.ready)broadcast.unshift({type:'sparklings',payload:this.union().join(',')});
  return {broadcast,private:privateFrames,leave};
 }
 replay(recipient:Attachment){
  const frames:unknown[]=[];if(this.ready&&recipient.bootstrap.stage==='done')frames.push({type:'sparklings',payload:this.union().join(',')});
  const seen=new Set<string>();for(const s of this.sessions){for(const e of s.evidence){const key=JSON.stringify(e);if(!seen.has(key)){seen.add(key);frames.push({type:'bootstrap-facts',payload:e});}}
   if(s.self===recipient.self)continue;for(const v of Object.values(s.snapshots)){if(v.kind==='S'&&!this.ready)continue;frames.push({type:'state',sender:s.self,sequence:v.sequence,kind:v.kind,payload:v.payload});}}
  return frames;
 }
}
