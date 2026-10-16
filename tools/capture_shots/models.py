"""Diagnostic visual baselines, never a calibrated launch or capture model."""
from dataclasses import dataclass
import math

@dataclass(frozen=True)
class Vec3:
    x:float
    y:float
    z:float

@dataclass(frozen=True)
class Sample:
    seconds:float
    position:Vec3
    source:str='raw'
    base_known:bool=True

@dataclass(frozen=True)
class VisualFit:
    origin_seconds:float
    position:Vec3
    velocity:Vec3
    acceleration:Vec3
    source:str='raw'

def components(v):return (v.x,v.y,v.z)

def fit_track(samples,degree):
    """Small normalized QR fit; reject missing/mixed evidence and deficient time bases."""
    if degree not in (1,2):raise ValueError('degree must be 1 or 2')
    samples=tuple(samples)
    if len(samples)<degree+1 or any(not s.base_known for s in samples) or len({s.source for s in samples})!=1:return None
    if any(not all(math.isfinite(v) for v in (s.seconds,*components(s.position))) for s in samples):return None
    if any(b.seconds<=a.seconds for a,b in zip(samples,samples[1:])):return None
    origin=samples[0].seconds;scale=samples[-1].seconds-origin
    if not math.isfinite(scale) or scale<=1e-12:return None
    times=[(s.seconds-origin)/scale for s in samples]
    q=[];r=[[0.0]*(degree+1) for _ in range(degree+1)]
    for j in range(degree+1):
        column=[t**j for t in times]
        for i,previous in enumerate(q):
            projection=math.fsum(a*b for a,b in zip(previous,column));r[i][j]=projection
            column=[a-projection*b for a,b in zip(column,previous)]
        norm=math.sqrt(math.fsum(v*v for v in column))
        if norm<1e-10:return None
        r[j][j]=norm;q.append([v/norm for v in column])
    coefficients=[]
    for axis in range(3):
        y=[components(s.position)[axis] for s in samples]
        rhs=[math.fsum(a*b for a,b in zip(col,y)) for col in q];c=[0.0]*(degree+1)
        for i in reversed(range(degree+1)):c[i]=(rhs[i]-math.fsum(r[i][j]*c[j] for j in range(i+1,degree+1)))/r[i][i]
        coefficients.append(c)
    position=Vec3(*(c[0] for c in coefficients));velocity=Vec3(*(c[1]/scale for c in coefficients))
    acceleration=Vec3(*(2*c[2]/(scale*scale) if degree==2 else 0.0 for c in coefficients))
    if not all(math.isfinite(v) for vec in (position,velocity,acceleration) for v in components(vec)):return None
    return VisualFit(origin,position,velocity,acceleration,samples[0].source)

def predict(fit,seconds):
    dt=seconds-fit.origin_seconds
    result=Vec3(*(p+v*dt+.5*a*dt*dt for p,v,a in zip(components(fit.position),components(fit.velocity),components(fit.acceleration))))
    if not all(math.isfinite(v) for v in components(result)):raise ValueError('nonfinite model prediction')
    return result

def residual_stats(errors):
    errors=tuple(errors)
    if any(not math.isfinite(e) or e<0 for e in errors):raise ValueError('invalid residual')
    ordered=sorted(errors)
    def percentile(p):return ordered[math.ceil(len(ordered)*p)-1] if ordered else None
    return {'count':len(errors),'rmse':math.hypot(*errors)/math.sqrt(len(errors)) if errors else None,
            'p50':percentile(.5),'p95':percentile(.95),'maximum':max(errors) if errors else None,
            'last_observed_error':errors[-1] if errors else None}
