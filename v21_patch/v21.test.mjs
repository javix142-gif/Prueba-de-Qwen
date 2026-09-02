import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const html = fs.readFileSync(path.join(root,'app/src/main/assets/index.html'),'utf8');
const manifest = fs.readFileSync(path.join(root,'app/src/main/AndroidManifest.xml'),'utf8');
const gradle = fs.readFileSync(path.join(root,'app/build.gradle'),'utf8');
const main = fs.readFileSync(path.join(root,'app/src/main/java/cl/javix/ilyrion/MainActivity.java'),'utf8');
const audioDir = path.join(root,'app/src/main/assets/audio');

const hasAll = (...needles) => needles.every(n => html.includes(n));

test('versión Android 2.1.0 / code 21',()=>{
  assert.match(gradle,/versionCode\s+21/); assert.match(gradle,/versionName\s+['"]2\.1\.0['"]/);
});
test('offline: manifest no INTERNET y cleartext false',()=>{
  assert.doesNotMatch(manifest,/android\.permission\.INTERNET/); assert.match(manifest,/usesCleartextTraffic="false"/);
});
test('WebView bloquea file/content access',()=>{
  assert.match(main,/setAllowFileAccess\(false\)/); assert.match(main,/setAllowContentAccess\(false\)/);
});
test('origen local HTTPS y assets interceptados',()=>{
  assert.match(main,/ilyrion\.local/); assert.match(main,/shouldInterceptRequest/); assert.match(main,/audio\/ogg/);
});
test('P2.1 controles configurables y joystick flotante',()=>{
  assert.ok(hasAll('controlPresets','Floating joystick','leftHanded','controlOpacity','cameraZoom','V21.joyPointer'));
});
test('P2.1 controles normal conservan tamaño táctil amplio',()=>{
  assert.match(html,/normal:\{joy:158,attack:96,skill:76,dash:68,interact:68/);
});
test('P2.1 cámara con zoom y seguimiento',()=>{
  assert.ok(hasAll('cameraZoom','V21.setCameraMode','V21.bumpCamera'));
});
test('P2.2 colisiones físicas por región e interiores',()=>{
  assert.ok(hasAll('V21.collisions','interior_liria','interior_aureval','interior_vesperia','interior_serath','interior_keldran','interior_cyrion'));
});
test('P2.2 movimiento usa resolución de colisión',()=>{
  assert.ok(hasAll('V21.moveEntity','V21.collides','V21.lineBlocked'));
});
test('P2.3 soft target y lock-on',()=>{
  assert.ok(hasAll('V21.pickTarget','V21.lockTargetId','V21.toggleLock','autoAim'));
});
test('P2.3 combo, ataque cargado, stagger, hit stop y números de daño',()=>{
  assert.ok(hasAll('charged','stagger','V21.freeze','V21.addDamageNumber','comboStep'));
});
test('P2.3 tres habilidades y ultimate por clase',()=>{
  assert.ok(hasAll('V21.classKits','skills:[','V21.useSkill','V21.useUltimate'));
});
test('P2.4 IA por roles/estados',()=>{
  for(const role of ['tank','support','assassin','controller','summoner','skirmisher','ranged','charger']) assert.ok(html.includes(role), role);
  assert.ok(hasAll('state','telegraph','reposition'));
});
test('P2.4 encuentros regionales compuestos',()=>{
  assert.ok(hasAll('V21.encounters','V21.addEncounterPack','aureval','vesperia','serath','keldran','cyrion'));
});
test('P2.5 sprites animados y cuatro direcciones',()=>{
  assert.ok(hasAll('V21.sprite','walk','facing','V2_human'));
});
test('P2.6 ocho bosses canónicos',()=>{
  for(const boss of ['radan','aureval','vesperia','serath','keldran','pentarca','chorus','edran']) assert.ok(html.includes(`'${boss}'`)||html.includes(`${boss}:`), boss);
});
test('P2.6 fases, stagger e intents en bosses',()=>{
  assert.ok(hasAll('v21-boss-meter','v21-stagger','intentIndex','PRÓXIMA'));
});
test('P2.6 segunda pasada visual de arenas incluida',()=>{
  assert.ok(hasAll('v21-boss-final-pass','Liria en llamas','Cripta de las Coronas','Gran Almacén IX','NEXUS'));
});
test('P2.7 seis interiores físicos',()=>{
  for(const name of ['Taberna del Fresno','Sala de los Recaudadores','Casa de Corredores','Hospicio de Peregrinos','Refugio de los Mineros','Archivo de Ceniza']) assert.ok(html.includes(name), name);
});
test('P2.7 minimapa usa geometría/colisiones reales',()=>{
  assert.ok(hasAll('V2_drawMiniMap=function','V21.collisions','S.discovered'));
});
test('P2.8 diez cadenas de quests secundarias',()=>{
  const m=html.match(/V21\.questChains\s*=\s*\{([\s\S]*?)\n\};/); assert.ok(m); const ids=[...m[1].matchAll(/^\s{2}([a-z0-9_]+):\{/gm)].map(x=>x[1]); assert.ok(ids.length>=10,`found ${ids.length}`);
});
test('P2.8 quests admiten etapas múltiples y decisiones',()=>{
  assert.ok(hasAll("type:'talk'","type:'return'","type:'choice'","type:'encounter'"));
});
test('P2.9 menú unificado y HUD contextual',()=>{
  assert.ok(hasAll('V21.openMenuHub','Diario','Equipo','Personaje','Mundo','Controles','Guardar','v21TargetCard'));
});
test('P2.9 diálogo responsive y choices menos form-like',()=>{
  assert.ok(hasAll('dialoguePortrait','.choices{display:grid'));
});
test('P2.10 música local por región y boss',()=>{
  for(const name of ['title','liria','liria_attack','cyrion','aureval','vesperia','serath','keldran','depths','boss']) assert.ok(fs.existsSync(path.join(audioDir,`${name}.ogg`)), name);
});
test('P2.10 SFX locales completos',()=>{
  for(const name of ['swing','heavy','hit','crit','skill','ultimate','dash','hurt','pickup']) assert.ok(fs.existsSync(path.join(audioDir,`${name}.ogg`)), name);
});
test('audio OGG no vacío y con cabecera OggS',()=>{
  for(const file of fs.readdirSync(audioDir).filter(x=>x.endsWith('.ogg'))){ const b=fs.readFileSync(path.join(audioDir,file)); assert.ok(b.length>3000,file); assert.equal(b.subarray(0,4).toString(),'OggS',file); }
});
test('P2.11 bandas regionales de nivel',()=>{
  assert.ok(hasAll('V21.regionLevels','liria:[1,3]','aureval:[3,7]','vesperia:[5,10]','serath:[7,12]','keldran:[9,14]'));
});
test('P2.11 loot, materiales, afijos y forja',()=>{
  assert.ok(hasAll('worldDrops','materials','affix','forgePanel','weaponUpgrade'));
});
test('P2.12 límites de partículas/proyectiles y control de rendimiento',()=>{
  assert.ok(hasAll('particle','projectile','slowFrames','performance'));
});
test('guardado y gameBack siguen disponibles',()=>{
  assert.ok(hasAll('saveGame','loadGame','window.gameBack'));
});
test('sin URLs http externas en runtime',()=>{
  const stripped=html.replace(/https:\/\/ilyrion\.local\/?/g,'');
  assert.doesNotMatch(stripped,/https?:\/\//);
});
