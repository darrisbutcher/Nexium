"""Make a transparent title PNG in the Dragonfall font.
   python3 custom_title.py "EPISODE 1: THE FALL" [scale=10] [green|gold|purple] [out.png]"""
import sys
from build import *
from font import G
text=sys.argv[1].upper(); scale=int(sys.argv[2]) if len(sys.argv)>2 else 10
style=sys.argv[3] if len(sys.argv)>3 else "green"; out=sys.argv[4] if len(sys.argv)>4 else "custom_title.png"
text="".join(c for c in text if c in G)
face,glowc={"green":(None,EM),"gold":(GOLDFACE,EMBER),"purple":(PURPFACE,PURPLE)}[style]
with_glow(title(text,scale,face=face),glowc,scale*2,1.3,pad=scale*8).save(out); print("saved",out)
