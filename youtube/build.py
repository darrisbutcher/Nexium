import random, math, os
from PIL import Image, ImageDraw, ImageFilter, ImageChops
import art
from art import *

OUT="out"; os.makedirs(OUT,exist_ok=True)
GOLDFACE=[WHITE,WHITE,(255,240,180),GOLD,GOLD,GOLD,(255,170,50),EMBER,EMBER,EMBER,(220,80,15),EMBER_D,EMBER_D,(110,25,8)]
PURPFACE=[WHITE,WHITE,(235,225,255),(200,175,255),(170,140,240),PURPLE,PURPLE,PURPLE,(90,50,160),VIOLET,VIOLET,(44,22,86),(44,22,86),(30,14,60)]

def outline_low(im,col=BLACK,r=1):
    out=Image.new("RGBA",(im.width+2*r,im.height+2*r),(0,0,0,0)); a=im.getchannel("A"); pad=Image.new("L",out.size,0); pad.paste(a,(r,r))
    mask=pad.filter(ImageFilter.MaxFilter(2*r+1)).point(lambda v:255 if v else 0)
    out.paste(Image.new("RGBA",out.size,col+(255,)),(0,0),mask); out.alpha_composite(im,(r,r)); return out

def bevel_panel(w,h,fill=(14,16,22,235),hi=EM,lo=EM_D,edge=BLACK,t=2):
    """Pixel-art UI frame (low-res units)."""
    im=Image.new("RGBA",(w,h),edge+(255,)); d=ImageDraw.Draw(im)
    d.rectangle([1,1,w-2,h-2],fill=fill)
    d.rectangle([1,1,w-2,1+t-1],fill=hi+(255,)); d.rectangle([1,1,1+t-1,h-2],fill=hi+(255,))
    d.rectangle([1,h-2-t+1,w-2,h-2],fill=lo+(255,)); d.rectangle([w-2-t+1,1,w-2,h-2],fill=lo+(255,))
    for cx,cy in((0,0),(w-1,0),(0,h-1),(w-1,h-1)): im.putpixel((cx,cy),(0,0,0,0))
    return im

def embers(w,h,box,n,seed,drift=(-1,1)):
    """Falling ember streaks trailing up-left from the dragon."""
    r=random.Random(seed); im=Image.new("RGBA",(w,h),(0,0,0,0)); px=im.load(); x0,y0,x1,y1=box
    for _ in range(n):
        x=r.randrange(x0,x1); y=r.randrange(y0,y1); L=r.randrange(2,9); c=r.choice([EMBER,EMBER,GOLD,EMBER_D,(255,150,60)])
        for i in range(L):
            xx,yy=int(x-i*drift[0]*-0.5),int(y+i*0.9)
            if 0<=xx<w and 0<=yy<h: px[xx,yy]=c+(max(40,255-i*34),)
    return im

def scene(wl=320,hl=180,dragon_xy=(150,26),dragon_scale=1.0,tilt=-14,seed=5,dim=0.0,moon=True,dragon=True):
    base=sky(wl,hl,seed)
    r=random.Random(seed)
    if moon:
        mx,my,mr=262,46,26
        md=Image.new("RGBA",(wl,hl),(0,0,0,0)); d=ImageDraw.Draw(md)
        for yy in range(-mr,mr+1):
            for xx in range(-mr,mr+1):
                q=math.hypot(xx,yy)
                if q<=mr:
                    t=(xx*.5+yy*.5)/mr*.5+.5
                    c=lerp(EM,VOID,min(1,max(0,t*1.1)))
                    if (xx*7+yy*13)%29==0 or ((xx+4)**2+(yy-6)**2<10): c=lerp(c,BLACK,.45)
                    d.point((mx+xx,my+yy),fill=c+(255,))
        g=glow(md,EM,8,1.0); base.alpha_composite(g)
        base.alpha_composite(md)
    # islands along the bottom
    isl=Image.new("RGBA",(wl,hl),(0,0,0,0))
    for cx,cy,rw,rh,col in [(30,150,46,26,0),(110,166,38,16,0),(210,158,50,20,0),(300,146,44,28,0),(160,178,30,10,0)]:
        for x in range(cx-rw,cx+rw+1):
            t=abs(x-cx)/rw; top=cy-int(3*(1-t)); bot=cy+int(rh*(1-t**1.5)*(0.85+0.15*r.random()))
            for y in range(top,bot+1):
                c=OBS if (y-top)>1 else MOSS
                if y==top: c=EM_D
                elif y==top+1: c=STONE
                elif (x*3+y*5)%11==0: c=STONE
                if 0<=x<wl and 0<=y<hl: isl.putpixel((x,y),c+(255,))
        k=r.randrange(1,3)
        for i in range(k):
            ox=cx+r.randrange(-rw//2,rw//2); cr=CRYSTAL; isl.alpha_composite(cr,(ox-cr.width//2,cy-3-cr.height+2))
    base.alpha_composite(isl)
    if dragon:
        d=DRAGON
        if dragon_scale!=1: d=d.resize((int(d.width*dragon_scale),int(d.height*dragon_scale)),Image.BOX); d.putalpha(d.getchannel('A').point(lambda v:255 if v>90 else 0))
        d=d.rotate(tilt,expand=True,resample=Image.NEAREST)
        dx,dy=dragon_xy
        base.alpha_composite(embers(wl,hl,(max(0,dx-30),dy+20,dx+d.width//2,dy+d.height+30),90,seed+1))
        base.alpha_composite(glow(d,EMBER,5,.35)); base.alpha_composite(d,(dx,dy))
    if dim:
        ov=Image.new("RGBA",(wl,hl),(4,3,10,int(255*dim))); base.alpha_composite(ov)
    return base

def finish(low,scale,bloom=True):
    big=up(low,scale)
    if bloom:
        b=big.convert("RGB").point(lambda v:max(0,v-150)).filter(ImageFilter.GaussianBlur(scale*3))
        big=Image.blend(big.convert("RGB"),ImageChops.add(big.convert("RGB"),b),.7).convert("RGBA")
    return big

def vignette(im,amt=.55):
    w,h=im.size; v=Image.new("L",(w,h),0); d=ImageDraw.Draw(v)
    d.ellipse([-w*.25,-h*.35,w*1.25,h*1.35],fill=255); v=v.filter(ImageFilter.GaussianBlur(w//10))
    dark=Image.new("RGBA",(w,h),(4,3,10,255)); mask=ImageChops.invert(v).point(lambda x:int(x*amt))
    im.paste(dark,(0,0),mask); return im

def logo(text="DRAGONFALL",scale=10,sub=None,subscale=None,glowcol=EM):
    t=title(text,scale); L=with_glow(t,glowcol,scale*2,1.3,pad=scale*8)
    if sub:
        s=title(sub,subscale or max(2,scale//3),face=GOLDFACE,extrude=2)
        c=Image.new("RGBA",(max(L.width,s.width+scale*16),L.height+s.height-scale*3),(0,0,0,0))
        c.alpha_composite(L,((c.width-L.width)//2,0))
        sg=with_glow(s,EMBER,scale,1.0,pad=scale*3); c.alpha_composite(sg,((c.width-sg.width)//2,L.height-scale*7))
        return c
    return L

def save(im,name): im.save(os.path.join(OUT,name)); print(name,im.size)

def make_episode_badges(n=100):
    d=os.path.join(OUT,"episode_badges"); os.makedirs(d,exist_ok=True)
    for i in range(1,n+1):
        t=title(f"EP {i}",9,face=GOLDFACE,extrude=2); with_glow(t,EMBER,16,1.1,pad=60).save(os.path.join(d,f"ep_{i:03d}.png"))
    print(n,"badges")

if __name__=="__main__":
    # ---- 1. logos (transparent) ----
    save(logo("DRAGONFALL",14,None),"02_logo_dragonfall.png")
    # ---- 2. banner 2560x1440 (safe area 1546x423 centre) ----
    low=scene(dragon_xy=(2,8),dragon_scale=.55,tilt=-12,seed=7,dim=.12)
    ban=finish(low,8); vignette(ban,.6)
    lg=logo("DRAGONFALL",8,None)
    paste_c(ban,lg,1280,720); save(ban.convert("RGB"),"04_channel_banner_2560x1440.png")
    # safe-area guide (for your reference)
    g=ban.copy(); d=ImageDraw.Draw(g); d.rectangle([1280-773,720-211,1280+773,720+211],outline=(255,60,60,255),width=4)
    d.rectangle([1280-1024,720-211,1280+1024,720+211],outline=(255,220,60,255),width=4); save(g.convert("RGB"),"04b_banner_safe_area_guide.png")
    # ---- 3. profile picture 800x800 ----
    pl=Image.new("RGBA",(100,100)); pl.alpha_composite(sky(100,100,11)); 
    ring=Image.new("RGBA",(100,100),(0,0,0,0)); dr=ImageDraw.Draw(ring)
    for rr,cc in((49,BLACK),(48,EM_D),(47,EM),(46,MINT),(45,EM),(44,EM_D),(43,BLACK)): dr.ellipse([50-rr,50-rr,50+rr-1,50+rr-1],outline=cc+(255,))
    pl.alpha_composite(ring)
    em=outline_low(wings_emblem(1)) if False else None
    wing_lo=art.wings_emblem(1); wing_lo=outline_low(wing_lo); 
    pl.alpha_composite(wing_lo.resize((wing_lo.width,wing_lo.height)),((100-wing_lo.width)//2,(100-wing_lo.height)//2+2))
    prof=finish(pl,8,bloom=True)
    mask=Image.new("L",(800,800),0); ImageDraw.Draw(mask).ellipse([0,0,799,799],fill=255)
    pr=prof.copy(); pr.putalpha(mask); save(prof.convert("RGB"),"05_profile_picture_800x800.png"); save(pr,"05b_profile_picture_round_transparent.png")
    # ---- 4. watermark 150x150 transparent ----
    cl=outline_low(CRYSTAL,BLACK,1); wm=Image.new("RGBA",(150,150),(0,0,0,0))
    big=up(cl,7); g=with_glow(big,EM,6,1.0,pad=20); wm.alpha_composite(g.crop(((g.width-150)//2,(g.height-150)//2,(g.width-150)//2+150,(g.height-150)//2+150)))
    save(wm,"06_video_watermark_150x150.png")
    # ---- 5. thumbnail template 1280x720 ----
    tl=scene(dragon_xy=(150,16),dragon_scale=1.0,tilt=-12,seed=9,dim=.05); th=finish(tl,4)
    th=vignette(th,.5)
    th.alpha_composite(logo("DRAGONFALL",8,None),(30,20) if False else (20,10))
    badge=outline_low(title("EP 1",1,face=GOLDFACE,extrude=1).resize((1,1))) if False else None
    save(th.convert("RGB"),"07_thumbnail_template_1280x720.png")
    # ---- 6. intro title card 1920x1080 ----
    il=scene(dragon_xy=(125,2),dragon_scale=.6,tilt=-10,seed=13,dim=.2); ic=finish(il,6); vignette(ic,.65)
    paste_c(ic,logo("DRAGONFALL",9,None),960,600); save(ic.convert("RGB"),"08_intro_title_card_1920x1080.png")
    # ---- 7. end screen 1920x1080 ----
    el=scene(dragon=False,seed=21,dim=.45); ec=finish(el,6); vignette(ec,.5)
    th2=title("THANKS FOR WATCHING",4); ec.alpha_composite(with_glow(th2,EM,8,1.0,pad=24),((1920-th2.width)//2-24,50))
    for (x,y,lab) in((120,260,"NEXT EPISODE"),(1040,260,"MORE DRAGONFALL")):
        pnl=up(bevel_panel(760//6+1,428//6+1,fill=(8,6,18,150)),6); ec.alpha_composite(pnl,(x-3,y-3))
        lt=title(lab,3,face=GOLDFACE,extrude=1); ec.alpha_composite(lt,(x+380-lt.width//2,y+440))
    cp=Image.new("RGBA",(40,40),(0,0,0,0)); d=ImageDraw.Draw(cp)
    for rr,cc in((19,BLACK),(18,EM),(17,EM_D),(16,(10,14,12))): d.ellipse([20-rr,20-rr,20+rr-1,20+rr-1],fill=cc+(255,))
    ec.alpha_composite(up(cp,5),(960-100,820-20)); st=title("SUBSCRIBE",3,face=GOLDFACE,extrude=1); ec.alpha_composite(st,(960-st.width//2,1010-st.height//2))
    save(ec.convert("RGB"),"09_end_screen_1920x1080.png")
    # ---- 8. lower third 1920x1080 transparent ----
    lt=Image.new("RGBA",(1920,1080),(0,0,0,0))
    p=up(bevel_panel(84,14,fill=(8,10,14,235)),8); lt.alpha_composite(with_glow(p,EM,10,.6,pad=30),(80-30,820-30))
    tt=title("DRAGONFALL",3,extrude=1); lt.alpha_composite(tt,(80+30,820+(p.height-tt.height)//2)); 
    cr=up(outline_low(CRYSTAL),5); lt.alpha_composite(cr,(80+p.width-cr.width-30,820+(p.height-cr.height)//2))
    save(lt,"10_lower_third_1920x1080.png")
    # ---- 9. episode badges 1-100 ----
    make_episode_badges()
