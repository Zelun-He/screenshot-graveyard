"""Rebuild the six original sample screenshots. Development only: pip install Pillow."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import math

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'app/src/main/assets/demo'
OUT.mkdir(parents=True, exist_ok=True)
FONT = Path('/usr/share/fonts/truetype/dejavu')

def font(size, kind='sans'):
    names={'sans':'DejaVuSans.ttf','bold':'DejaVuSans-Bold.ttf','serif':'DejaVuSerif.ttf','mono':'DejaVuSansMono.ttf'}
    return ImageFont.truetype(str(FONT/names[kind]), size)

def base(bg):
    im=Image.new('RGB',(540,810),bg)
    return im,ImageDraw.Draw(im)

def txt(d,xy,text,size=22,fill='#242029',kind='sans',spacing=10):
    d.multiline_text(xy,text,font=font(size,kind),fill=fill,spacing=spacing)

# 1: an illustrated recipe clipping, readable without a network or external assets.
im,d=base('#F2EBD9')
txt(d,(36,34),'Midnight miso ramen',30,kind='serif')
txt(d,(36,84),'THE MIDNIGHT KITCHEN  /  15 MIN',12,fill='#7B695A',kind='bold')
d.rounded_rectangle((26,122,514,422),24,fill='#E4BB79')
d.ellipse((104,172,437,397),fill='#B35C32')
d.ellipse((111,163,430,345),fill='#F4DFA9')
d.ellipse((125,175,416,332),fill='#925739')
for i in range(11):
 y=207+i*8
 d.arc((156,y-20,366,y+40),0,170,fill='#E6B85B',width=4)
d.ellipse((289,210,370,280),fill='#F5E5B9');d.ellipse((310,227,346,261),fill='#EAA047')
for i in range(8):
 x=166+i*12
 d.line((x,218,x+20,196),fill='#5B7348',width=6)
d.line((365,147,468,112),fill='#5C3F2E',width=7);d.line((370,159,479,127),fill='#5C3F2E',width=7)
txt(d,(36,455),'For the nights you need a little comfort.',18)
txt(d,(36,505),'INGREDIENTS',13,fill='#9A633E',kind='bold')
txt(d,(36,543),'1 tbsp miso paste\n2 cups vegetable broth\n1 pack ramen noodles\n1 egg + a handful of scallions',21,spacing=14)
txt(d,(36,733),'Save this recipe for a rainy evening.',15,fill='#7B695A')
im.save(OUT/'1.png',optimize=True)
# 2: code note
im,d=base('#172324')
txt(d,(34,36),'Git survival kit',32,'#D3E9D3','serif')
txt(d,(34,90),'A NOTE TO FUTURE ME',12,'#8FA89E','bold')
for y in [145,315,490]: d.rounded_rectangle((24,y,516,y+145),16,fill='#213435')
txt(d,(43,166),'SAVE YOUR WORK',12,'#B5D28A','bold')
txt(d,(43,205),'git stash push -m "wip"\ngit stash pop',20,'#EFF0DC','mono',14)
txt(d,(43,337),'UNDO THE LAST COMMIT',12,'#B5D28A','bold')
txt(d,(43,376),'git reset --soft HEAD~1',19,'#EFF0DC','mono')
txt(d,(43,412),'Keeps your changes staged.',16,'#8FA89E')
txt(d,(43,512),'FIND THE LOST THING',12,'#B5D28A','bold')
txt(d,(43,551),'git reflog\ngit switch -c rescue HASH',18,'#EFF0DC','mono',14)
txt(d,(35,706),'Small commands. Enormous relief.',18,'#BDD1C6','serif')
im.save(OUT/'2.png',optimize=True)
# 3: coffee bookmark
im,d=base('#DDDBD1')
txt(d,(36,34),'A little coffee detour',29,kind='serif')
d.rounded_rectangle((24,99,516,442),22,fill='#719084')
d.ellipse((122,351,416,394),fill='#527368')
d.ellipse((151,199,395,361),fill='#F1ECE1')
d.ellipse((171,208,375,320),fill='#C49A70')
d.ellipse((185,215,361,309),fill='#73513A')
d.arc((340,233,446,335),250,110,fill='#F1ECE1',width=18)
for x in [233,273,310]:d.arc((x,133,x+26,204),90,270,fill='#AFC7BB',width=4)
txt(d,(36,476),'FERN & FABLE',27,kind='serif')
txt(d,(36,529),'Coffee, books, and an unhurried afternoon.',18)
txt(d,(36,586),'Saturday plan\nOrder an oat milk latte.\nBring the book you keep postponing.',20,spacing=15)
txt(d,(36,745),'FICTIONAL CAFE  /  SAMPLE BOOKMARK',11,fill='#6D756B',kind='bold')
im.save(OUT/'3.png',optimize=True)
# 4: event card
im,d=base('#27213B')
txt(d,(36,36),'A night worth saving',29,'#E8D9BF','serif')
d.rounded_rectangle((27,110,513,737),24,fill='#B7A3DA')
txt(d,(56,147),'SMALL HOURS',41,'#30233F','serif')
txt(d,(56,209),'LIVE IN THE COURTYARD',13,'#51405C','bold')
for i in range(6):
 x=118+i*54
 d.line((x,310,x-42,478),fill='#7D659D',width=2)
 d.ellipse((x-18,294,x+18,330),fill='#F0DBA4')
d.ellipse((92,419,448,560),fill='#463554')
txt(d,(56,591),'FRIDAY / DOORS OPEN 7 PM',15,'#30233F','bold')
txt(d,(56,628),'Bring a friend. Stay for the encore.',17,'#30233F')
txt(d,(56,690),'SAMPLE EVENT  /  NOT A VALID TICKET',10,'#51405C','bold')
im.save(OUT/'4.png',optimize=True)
# 5: travel moodboard
im,d=base('#E4E5D7')
txt(d,(35,35),'Kyoto, someday.',35,kind='serif')
txt(d,(36,93),'A VERY UNFINISHED ITINERARY',12,'#68735E','bold')
d.rounded_rectangle((25,135,515,455),20,fill='#A6B497')
for i in range(7):
 x=74+i*65
 d.rectangle((x,140,x+11,454),fill='#617B57')
 for y in [185,268,350]:d.line((x-3,y,x+16,y),fill='#80936E',width=4)
d.polygon([(220,455),(319,455),(282,235),(261,235)],fill='#DBCEAB')
txt(d,(36,495),'DAY 01 / SLOW BEGINNINGS',14,'#61734F','bold')
txt(d,(36,537),'Bamboo grove before breakfast\nCoffee somewhere quiet\nWalk until something looks interesting',19,spacing=14)
txt(d,(36,680),'Hotel? Later.\nFlight? Also later.\nPossibility? Absolutely.',18,'#68735E',spacing=10)
im.save(OUT/'5.png',optimize=True)
# 6: saved thought
im,d=base('#C6B6D8')
txt(d,(36,36),'A thought to keep',16,'#51435F','bold')
d.line((36,88,504,88),fill='#9A87B0',width=1)
txt(d,(40,166),'Make room\nfor the\npossibility\nthat it\nworks out.',50,'#30263D','serif',15)
d.ellipse((401,622,459,680),outline='#7B638F',width=2)
d.line((430,608,430,695),fill='#7B638F',width=2);d.line((386,651,474,651),fill='#7B638F',width=2)
txt(d,(38,745),'REMEMBER THIS WHEN YOU CREATE.',12,'#51435F','bold')
im.save(OUT/'6.png',optimize=True)
print('Generated',len(list(OUT.glob('*.png'))),'sample screenshots')
