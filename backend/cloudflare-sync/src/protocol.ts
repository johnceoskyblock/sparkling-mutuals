export const MAX_SEQUENCE=9007199254740991;
export function check(value: unknown, condition: boolean): asserts condition { if(!condition)throw new Error('Invalid protocol'); }
export function object(v:unknown):Record<string,unknown>{check(v,!!v&&typeof v==='object'&&!Array.isArray(v));return v as Record<string,unknown>;}
export function integer(v:unknown,min=0,max=MAX_SEQUENCE):number{check(v,typeof v==='number'&&Number.isSafeInteger(v)&&v>=min&&v<=max);return v as number;}
export function text(v:unknown,max=128):string{check(v,typeof v==='string'&&v.length>0&&v.length<=max);return v as string;}
export function uuid(v:unknown):string{const s=text(v,32);check(v,/^[0-9a-f]{32}$/.test(s));return s;}
export function identity(l:unknown,s:unknown,r:unknown){
 let lobby=text(l,64).toLowerCase();check(l,/^[a-z0-9_-]+$/.test(lobby));lobby=lobby.replace(/^mini([0-9][a-z0-9]*)$/,'m$1');
 const self=uuid(s);check(r,Array.isArray(r)&&r.length>=2&&r.length<=4);const roster=(r as unknown[]).map(uuid).sort();
 check(r,new Set(roster).size===roster.length&&roster.includes(self));return {lobby,self,roster};
}
export function numbers(kind:string,payload:unknown):number[]{
 check(payload,typeof payload==='string'&&payload.length<=3072);
 const parts=(payload as string).split(',');check(parts,parts.every(p=>/^-?[0-9]+$/.test(p)));
 const v=parts.map(Number), aggregate=kind==='A', version=v.length===39?3:2;
 const range=(n:number,a:number,b:number)=>Array.from({length:n},()=>[a,b]);
 const bounds=aggregate?[...range(9,0,256),...range(4,0,64),...range(4,0,256),[-1,2],[0,64],...range(4,0,64),[0,7],[0,4],...range(8,0,1),[version,version],[0,31],[0,7],[0,65535],[0,65535],...(version===3?[[0,64]]:[])]:
 kind==='N'?[[0,8191]]:kind==='S'?[[0,1073741823],[0,127],[0,1073741823],[0,127]]:kind==='U'?[...range(4,0,256),[-1,2],[0,64]]:[];
 check(v,bounds.length>0&&v.length===bounds.length);v.forEach((n,i)=>integer(n,bounds[i][0],bounds[i][1]));
 if(aggregate)check(v,!(v[18]>0&&v[17]<0));if(kind==='U')check(v,!(v[5]>0&&v[4]<0));return v;
}
export const BIOMES=['FOREST','CAVERN','ICY','HAUNTED'] as const;
export type Biome=typeof BIOMES[number];
export function biome(v:unknown):Biome{check(v,BIOMES.includes(v as Biome));return v as Biome;}
export const CODES=['fd','cd','id','hd'];
export function locks(v:unknown,roster:string[]):Record<string,Biome>{const o=object(v),out:Record<string,Biome>={};for(const [id,b] of Object.entries(o)){check(id,roster.includes(uuid(id)));out[id]=biome(b);}check(v,new Set(Object.values(out)).size===Object.keys(out).length);return out;}
