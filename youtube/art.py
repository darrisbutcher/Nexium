"""Dragonfall x Nexium channel art. Everything is generated from code in this folder: no stock art, no outside fonts."""
import random, math
from PIL import Image, ImageFilter, ImageDraw, ImageChops
from font import bitmap

# ---- palette (sampled from the Nexium ingot/block/icon textures + our own accents)
BLACK=(8,11,9); OBS=(21,28,24); STONE=(33,44,38); MOSS=(51,66,57)
EM_D=(31,127,82); EM=(34,176,122); MINT=(157,255,208); WHITE=(236,255,246)
VOID_D=(8,6,22); VOID=(26,12,52); VIOLET=(58,30,110); PURPLE=(110,60,190)
EMBER_D=(150,34,10); EMBER=(255,110,20); GOLD=(255,200,70)

def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
def grad(stops,t):
    t=max(0,min(1,t))
    for i in range(len(stops)-1):
        t0,c0=stops[i]; t1,c1=stops[i+1]
        if t<=t1: return lerp(c0,c1,(t-t0)/(t1-t0) if t1>t0 else 0)
    return stops[-1][1]
def up(im,s): return im.resize((im.width*s,im.height*s),Image.NEAREST)
def glow(im,color,radius,strength=1.0):
    a=im.split()[3].filter(ImageFilter.GaussianBlur(radius)).point(lambda v:min(255,int(v*strength)))
    out=Image.new("RGBA",im.size,color+(0,)); out.putalpha(a); return out

# ---- sprites (hand-drawn, right half only where symmetric)
SPR_PAL={'#':BLACK,'o':OBS,'s':STONE,'m':MOSS,'d':EM_D,'e':EM,'l':MINT,'w':WHITE,'v':VIOLET,'p':PURPLE,'f':EMBER,'g':GOLD,'r':EMBER_D}
def sprite(rows,mirror=False):
    if mirror: rows=[r+r[::-1] for r in rows]
    w=max(len(r) for r in rows); im=Image.new("RGBA",(w,len(rows)),(0,0,0,0)); px=im.load()
    for y,r in enumerate(rows):
        for x,c in enumerate(r):
            if c in SPR_PAL: px[x,y]=SPR_PAL[c]+(255,)
    return im

# Nexium crystal: tall faceted gem (mirrored)
CRYSTAL=sprite([
"......##",
".....#ll",
"....#lle",
"...#llee",
"..#lleed",
"..#leeed",
".#lleedd",
".#leeedd",
".#leedd#",
"..#eedd#",
"..#edd#.",
"...#dd#.",
"....##..",
],mirror=True)

def make_dragon(w=140,h=86,body=(30,24,52),mem=(66,34,124),far=(40,20,80),rim=EM,eye=MINT):
    """Dragon silhouette facing right, wings raised. Polygons rasterised with no anti-aliasing = pixel art."""
    def wing(dx,dy,mc,bc):
        l=Image.new("RGBA",(w,h),(0,0,0,0)); d=ImageDraw.Draw(l)
        T=lambda p:(p[0]+dx,p[1]+dy)
        S,Wr=(64,44),(56,12); tips=[(24,0),(10,20),(16,40),(34,52)]
        mid=[(34,14),(26,30),(26,46)]
        poly=[S,Wr,tips[0],mid[0],tips[1],mid[1],tips[2],mid[2],tips[3],(52,54)]
        d.polygon([T(p) for p in poly],fill=mc+(255,))
        for t in tips: d.line([T(Wr),T(t)],fill=bc+(255,),width=1)
        d.line([T(S),T(Wr)],fill=bc+(255,),width=4); d.line([T(Wr),T(tips[0])],fill=bc+(255,),width=2)
        return l
    im=Image.new("RGBA",(w,h),(0,0,0,0))
    im.alpha_composite(wing(18,-4,far,(26,12,56)))
    l=Image.new("RGBA",(w,h),(0,0,0,0)); d=ImageDraw.Draw(l); P=lambda pts:d.polygon(pts,fill=body+(255,))
    P([(48,54),(30,60),(16,68),(6,70),(0,80),(8,74),(16,78),(14,70),(26,66),(42,64),(58,62)])           # tail + spade
    P([(40,44),(60,36),(78,34),(90,32),(96,24),(106,22),(112,30),(104,40),(98,46),(88,54),(70,62),(52,64),(40,58)]) # torso/neck
    P([(92,26),(104,16),(118,18),(132,28),(137,31),(132,34),(120,34),(112,40),(100,42),(92,36)])        # skull/snout
    P([(100,18),(90,8),(78,3),(88,12),(94,22)]); P([(108,17),(100,6),(92,1),(102,10),(112,19)])        # horns
    P([(72,58),(66,72),(74,72),(78,62)]); P([(56,60),(50,72),(58,72),(64,62)])                          # legs
    P([(66,72),(62,77),(72,72)]); P([(50,72),(46,77),(56,72)])
    for i in range(5): P([(58+i*8,36-i*1.2),(61+i*8,28-i*1.2),(65+i*8,34-i*1.2)])                       # back spines
    im.alpha_composite(l); body_mask=l
    im.alpha_composite(wing(0,0,mem,(36,18,72)))
    px=im.load(); out=im.copy(); op=out.load()
    for y in range(1,h-1):
        for x in range(w):
            if not px[x,y][3]: continue
            if not px[x,y-1][3]: op[x,y]=rim+(255,)
            elif y>1 and not px[x,y-2][3]: op[x,y]=lerp(px[x,y][:3],rim,.35)+(255,)
            elif not px[x,y+1][3] and body_mask.getpixel((x,y))[3]: op[x,y]=lerp(px[x,y][:3],EMBER,.6)+(255,)
    for dx,dy in((0,0),(1,0),(2,0)): op[118+dx,24+dy]=eye+(255,)
    op[119,25]=WHITE+(255,)
    return out
DRAGON=make_dragon()

def starfield(w,h,n,seed):
    r=random.Random(seed); im=Image.new("RGBA",(w,h),(0,0,0,0)); px=im.load()
    for _ in range(n):
        x,y=r.randrange(w),r.randrange(h); c=r.choice([WHITE,MINT,(200,180,255),(255,230,200)])
        px[x,y]=c+(r.choice([90,140,255]),)
        if r.random()<.06:
            for dx,dy in((1,0),(-1,0),(0,1),(0,-1)):
                if 0<=x+dx<w and 0<=y+dy<h: px[x+dx,y+dy]=c+(110,)
    return im

def sky(w,h,seed=1):
    im=Image.new("RGBA",(w,h)); px=im.load(); r=random.Random(seed)
    stops=[(0,(6,4,18)),(.45,VOID),(.8,(40,22,82)),(1,(22,70,60))]
    for y in range(h):
        base=grad(stops,y/h)
        for x in range(w):
            # ordered-dither banding for the pixel-art look
            d=((x%2)+(y%2)*2)/4-.375
            c=lerp(base,grad(stops,(y+4)/h),.5+d)
            px[x,y]=c+(255,)
    im.alpha_composite(starfield(w,h,int(w*h/90),seed))
    return im

def end_islands(w,h,seed=3):
    """Floating end-style islands in obsidian/moss, lit emerald on top edges."""
    r=random.Random(seed); im=Image.new("RGBA",(w,h),(0,0,0,0)); d=ImageDraw.Draw(im)
    def island(cx,cy,rw,rh):
        for x in range(cx-rw,cx+rw+1):
            t=abs(x-cx)/rw; top=cy-int(2*(1-t)); bot=cy+int(rh*(1-t**1.6)*(0.8+0.2*r.random()))
            for y in range(top,bot+1):
                col=OBS if (y-top)>1 else MOSS
                if y==top: col=EM_D
                elif (x+y)%7==0: col=STONE
                if 0<=x<w and 0<=y<h: im.putpixel((x,y),col+(255,))
    return im,island

def title(text,scale,face=None,extrude=2,outline=1,sub=None):
    """Chunky 3D block title, returned RGBA at final resolution (transparent bg)."""
    m0=bitmap(text,gap=2)
    m=[[v for v in row for _ in range(2)] for row in m0 for _ in range(2)]   # 2x2 fat pixels
    H=len(m); W=len(m[0]); pad=extrude+outline+2
    lw,lh=W+pad*2,H+pad*2
    face=face or [MINT,MINT,(150,250,200),(110,240,186),(70,215,150),EM,EM,EM,(28,150,100),EM_D,EM_D,(25,105,70),(25,105,70),(20,90,60)]
    def at(x,y): return 0<=y<H and 0<=x<W and m[y][x]
    low=Image.new("RGBA",(lw,lh),(0,0,0,0)); px=low.load()
    on=lambda x,y: at(x-pad,y-pad)
    # outline (dilate union of face+extrusion)
    def solid(x,y): return any(on(x-d,y-d) for d in range(extrude+1))
    for y in range(lh):
        for x in range(lw):
            if any(solid(x+dx,y+dy) for dx in range(-outline,outline+1) for dy in range(-outline,outline+1)):
                px[x,y]=BLACK+(255,)
    for d in range(extrude,0,-1):
        for y in range(lh):
            for x in range(lw):
                if on(x-d,y-d): px[x,y]=lerp(EM_D,BLACK,.35+.15*d/extrude)+(255,) if d<extrude else lerp(OBS,BLACK,.3)+(255,)
    for y in range(lh):
        for x in range(lw):
            if on(x,y):
                ry=y-pad; c=face[ry]
                if not on(x,y-1): c=WHITE if ry==0 else lerp(c,WHITE,.55)
                elif not on(x,y+1): c=lerp(c,BLACK,.35)
                if not on(x+1,y): c=lerp(c,BLACK,.25)
                if not on(x-1,y) and on(x,y-1) is False: pass
                px[x,y]=c+(255,)
    big=up(low,scale)
    return big

def with_glow(im,color,radius,strength=1.4,pad=None):
    pad=pad or radius*3
    c=Image.new("RGBA",(im.width+pad*2,im.height+pad*2),(0,0,0,0)); c.paste(im,(pad,pad))
    out=glow(c,color,radius,strength); out.alpha_composite(glow(c,color,radius//3,.8)); out.alpha_composite(c); return out

def paste_c(dst,src,cx,cy): dst.alpha_composite(src,(int(cx-src.width/2),int(cy-src.height/2)))

def wings_emblem(scale):
    """Crest: Nexium crystal flanked by a pair of spread wings (all pixel-drawn)."""
    wing=sprite([
"..............................##",
".............................#pp",
"..........................###ppv",
"......................####pppppvv",
"...................###ppppvvvvvv#",
"................###pppvvvvvvvv##.",
"..............##ppvvvvvvvvvv##...",
".............#ppvvv#vvvvvv##....",
"............#pvvv##.#vvvv#......",
"...........#pvv#....#vvv#.......",
"..........#pv#......#vv#........",
".........#pv#........#v#........",
"........#p##..........##........",
"........##......................",
])
    wl=wing; wr=wing.transpose(Image.FLIP_LEFT_RIGHT)
    gap=2; w=wl.width*2+CRYSTAL.width+gap*2; h=max(CRYSTAL.height+4,wl.height+4)
    im=Image.new("RGBA",(w,h),(0,0,0,0))
    im.alpha_composite(wl,(0,2)); im.alpha_composite(wr,(w-wr.width,2))
    im.alpha_composite(CRYSTAL,(wl.width+gap,(h-CRYSTAL.height)//2))
    return up(im,scale)
