import {BIOMES,Biome,biome,check,locks as parseLocks,object,uuid,integer} from './protocol';
export function javaHash(s:string):number{let h=0;for(let i=0;i<s.length;i++)h=(Math.imul(h,31)+s.charCodeAt(i))|0;return h;}
const order=(a:string,b:string)=>javaHash(a)-javaHash(b)||a.localeCompare(b);
interface Hello {display:boolean;ticketed?:boolean;leader:boolean;present:string[];locked:Record<string,Biome>}
interface Member {present:boolean;ticketed:boolean;display:boolean;displayed:boolean;left:boolean;connected:boolean}
export interface Fairness {run:string;revision:number;counts:Record<string,number>}
export class Allocation {
 run=crypto.randomUUID();revision=0;locks:Record<string,Biome>={};fairness:Record<string,number>={};conflict=false;initialDone=false;due:number|null=null;
 reports:Record<string,Biome>={};firstReport:number|null=null;thirdReport:number|null=null;leader:string|null=null;
 members:Record<string,Member>={};assignedAt:Record<string,number>={};acknowledged:string[]=[];instructed:Record<string,Biome>={};seeded:Fairness|null=null;
 backupWins:Record<string,number>={};
 constructor(public roster:string[],backupWins:Record<string,number>={}){this.backupWins={...backupWins};this.fairness={...backupWins};}
 static restore(value:unknown):Allocation{const v=value as Allocation;return Object.assign(new Allocation(v.roster),v);}
 toJSON(){return {...this};}
 importLocks(next:Record<string,Biome>){
  const values=parseLocks(next,this.roster);for(const [id,b] of Object.entries(values)){
   if((this.locks[id]&&this.locks[id]!==b)||Object.entries(this.locks).some(([other,v])=>other!==id&&v===b)){this.conflict=true;continue;}this.locks[id]=b;
  }
 }
 seed(next:Fairness){
  check(next,typeof next.run==='string'&&next.run.length<=128);integer(next.revision);Object.entries(next.counts).forEach(([id,n])=>{check(id,this.roster.includes(uuid(id)));integer(n,0,1000000);});
  const old=this.seeded;
  if(old&&old.run!==next.run){this.conflict=true;return;}
  if(old&&old.revision===next.revision&&this.roster.some(id=>(old.counts[id]??0)!==(next.counts[id]??0))){this.conflict=true;return;}
  if(old&&old.revision>=next.revision)return;
  // Reconciliation precedes any awarded wins; never add copies from different clients.
  if(this.initialDone){this.conflict=true;return;}this.seeded=structuredClone(next);this.fairness=Object.fromEntries(this.roster.map(id=>[id,(next.counts[id]??0)+(this.backupWins[id]??0)]));
 }
 hello(id:string,input:unknown,now:number){
  const h=object(input);check(h,typeof h.display==='boolean'&&typeof h.leader==='boolean'&&(h.ticketed===undefined||typeof h.ticketed==='boolean'));
  check(h,Array.isArray(h.present)&&h.present.length<=4);const present=(h.present as unknown[]).map(uuid);check(h,present.every(p=>this.roster.includes(p)));
  this.importLocks(parseLocks(h.locked,this.roster));
  for(const p of [id,...present]){check(p,this.roster.includes(p));this.members[p]??={present:true,ticketed:true,display:false,displayed:false,left:false,connected:false};if(!this.members[p].left)this.members[p].present=true;}
  const m=this.members[id];if(m.left)return;
  Object.assign(m,{connected:true,display:h.display,ticketed:h.ticketed!==false});if(m.display&&this.locks[id])m.displayed=true;
  if(h.leader)this.leader=id;else if(this.leader===id)this.leader=null;
  if(this.initialDone&&this.eligible().some(p=>!this.locks[p]))this.due??=now+250;
  this.schedule(now);this.revision++;
 }
 report(id:string,b:Biome,now:number){check(id,this.roster.includes(id));biome(b);this.reports[id]??=b;this.schedule(now);this.revision++;}
 private eligible(){return this.roster.filter(id=>this.members[id]?.present&&this.members[id].ticketed&&!this.members[id].left);}
 private schedule(now:number){
  if(this.initialDone)return;const eligible=this.eligible(),reported=eligible.filter(id=>this.reports[id]);
  if(reported.length)this.firstReport??=now;
  if(reported.length>=3||(eligible.length<=2&&eligible.length>0&&reported.length===eligible.length))this.thirdReport??=now;
  this.due=this.firstReport===null?null:Math.min(this.firstReport+5000,this.thirdReport===null?Infinity:this.thirdReport+2000);
 }
 allocate(now:number,ready=true):boolean{
  if(!ready||this.conflict||this.due===null||now<this.due)return false;
  const pending=this.eligible().filter(id=>!this.locks[id]);const available=BIOMES.filter(b=>!Object.values(this.locks).includes(b));
  for(const b of available){const candidates=pending.filter(id=>this.reports[id]===b).sort((a,b)=>(this.fairness[a]??0)-(this.fairness[b]??0)||order(a,b));
   if(candidates.length){const id=candidates[0];this.locks[id]=b;this.fairness[id]=(this.fairness[id]??0)+1;this.backupWins[id]=(this.backupWins[id]??0)+1;}}
  const rest=pending.filter(id=>!this.locks[id]).sort(order),free=BIOMES.filter(b=>!Object.values(this.locks).includes(b));
  rest.forEach((id,i)=>{if(free[i])this.locks[id]=free[i];});
  for(const id of pending)if(this.locks[id]){this.assignedAt[id]=now;if(this.members[id].display)this.members[id].displayed=true;}
  this.initialDone=true;this.due=null;this.revision++;return true;
 }
 leave(id:string){if(this.members[id]){this.members[id].left=true;this.members[id].connected=false;}if(this.leader===id)this.leader=null;this.revision++;}
 disconnect(id:string){if(this.members[id])this.members[id].connected=false;if(this.leader===id)this.leader=null;this.revision++;}
 ack(sender:string,id:string){check(id,sender===this.leader&&id.startsWith(this.run+':'));const player=id.slice(this.run.length+1);check(player,!!this.locks[player]);if(!this.acknowledged.includes(id))this.acknowledged.push(id);this.instructed[player]=this.locks[player];this.revision++;}
 snapshot(now:number,recipient:string,ready=true){
  const notifications=this.conflict||!ready||this.leader!==recipient||!this.members[recipient]?.connected?[]:Object.entries(this.locks).filter(([id])=>{
   const m=this.members[id];return m?.present&&m.ticketed&&!m.left&&!m.displayed&&!m.display&&now-(this.assignedAt[id]??now)>=2000&&!this.acknowledged.includes(`${this.run}:${id}`)&&this.instructed[id]!==this.locks[id];
  }).map(([player,b])=>({id:`${this.run}:${player}`,player,biome:b}));
  return {version:1,run:this.run,revision:this.revision,assignments:this.locks,fairness:this.fairness,leader:this.leader,notifications,conflict:this.conflict,initialDone:this.initialDone};
 }
}
