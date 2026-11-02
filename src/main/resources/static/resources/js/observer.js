/*
 * Observer chase (/painel/observer). The stolen car is the subject: while it moves it reports its
 * position by radio and calls update() on every observer in its list, just like
 * demos/observer/PoliceChase.java. Police cars chase the last reported position; the helicopter only
 * lights it up. Detached observers stop receiving reports.
 */
(function () {
  'use strict';

  const W = 960;
  const H = 600;
  const RADIO_INTERVAL = 0.5;
  const THIEF_SPEED = 210;
  const POLICE_SPEED = 178;
  const HELI_SPEED = 300;
  const CATCH_DISTANCE = 30;
  const MAX_POLICE = 5;
  const SPAWNS = [[90, 90], [870, 510], [870, 90], [90, 510], [480, 50]];

  // Real lines of demos/observer/PoliceChase.java
  const SOURCE = [
    [6, '\tinterface Observer {'],
    [8, '\t\tvoid update(StolenCar car);'],
    [10, '\t}'],
    [12, '\tstatic class StolenCar {'],
    [14, '\t\tprivate final List<Observer> observers = new ArrayList<>();'],
    [26, '\t\tvoid attach(Observer observer) {'],
    [27, '\t\t\tthis.observers.add(observer);'],
    [28, '\t\t}'],
    [29, ''],
    [30, '\t\tvoid detach(Observer observer) {'],
    [31, '\t\t\tthis.observers.remove(observer);'],
    [32, '\t\t}'],
    [33, ''],
    [34, '\t\tvoid move(int dx, int dy) {'],
    [35, '\t\t\tthis.x += dx;'],
    [36, '\t\t\tthis.y += dy;'],
    [37, '\t\t\tnotifyObservers();'],
    [38, '\t\t}'],
    [39, ''],
    [40, '\t\tprivate void notifyObservers() {'],
    [41, '\t\t\tfor (Observer observer : this.observers) {'],
    [42, '\t\t\t\tobserver.update(this);'],
    [43, '\t\t\t}'],
    [44, '\t\t}'],
    [60, '\tstatic class PoliceCar implements Observer {'],
    [68, '\t\t@Override'],
    [69, '\t\tpublic void update(StolenCar car) {'],
    [70, '\t\t\tSystem.out.printf("%s: carro %s visto em (%d, %d), indo para lá%n", ...);'],
    [72, '\t\t}'],
    [76, '\tstatic class Helicopter implements Observer {'],
    [78, '\t\t@Override'],
    [79, '\t\tpublic void update(StolenCar car) {'],
    [80, '\t\t\tSystem.out.printf("Helicóptero: iluminando o carro %s em (%d, %d)%n", ...);'],
    [81, '\t\t}']
  ];

  const LINES = {
    attach: [26, 27, 28],
    detach: [30, 31, 32],
    move: [34, 35, 36, 37],
    notify: [40, 41, 42],
    police: [69, 70],
    heli: [79, 80]
  };

  const canvas = document.getElementById('chase-canvas');
  const ctx = canvas.getContext('2d');
  const overlay = document.getElementById('chase-overlay');
  const listView = document.getElementById('chase-list');
  const unitButtons = document.getElementById('chase-buttons');
  const logView = document.getElementById('chase-log');
  const addPoliceButton = document.getElementById('add-police');
  const heliButton = document.getElementById('toggle-heli');
  const hud = {
    notifications: document.getElementById('hud-notifications'),
    observers: document.getElementById('hud-observers'),
    time: document.getElementById('hud-time')
  };

  // ---------- the pattern ----------

  class StolenCar {

    constructor(x, y) {
      this.observers = [];
      this.plate = 'ROB-0001';
      this.x = Math.round(x);
      this.y = Math.round(y);
    }

    attach(observer) {
      this.observers.push(observer);
      onAttach(observer);
    }

    detach(observer) {
      this.observers.splice(this.observers.indexOf(observer), 1);
      onDetach(observer);
    }

    move(dx, dy) {
      this.x += dx;
      this.y += dy;
      onMove(dx, dy);
      this.notifyObservers();
    }

    notifyObservers() {
      onNotify(this.observers.length);
      for (const observer of this.observers) {
        observer.update(this);
      }
    }

  }

  class PoliceCar {

    constructor(number, x, y) {
      this.number = number;
      this.name = 'Viatura ' + number;
      this.variable = 'viatura' + number;
      this.kind = 'police';
      this.x = x;
      this.y = y;
      this.angle = Math.atan2(H / 2 - y, W / 2 - x);
      this.speed = 0;
      this.target = null;
      this.flash = 0;
    }

    update(car) {
      this.target = { x: car.x, y: car.y };
      this.flash = 0.45;
      beam(this);
      log('sub', this.variable + '.update(car) → vai até (' + car.x + ', ' + car.y + ')');
      hot('police');
    }

    step(dt) {
      this.flash = Math.max(0, this.flash - dt);
      let wanted = 0;
      if (this.target) {
        const dx = this.target.x - this.x;
        const dy = this.target.y - this.y;
        const distance = Math.hypot(dx, dy);
        if (distance > 14) {
          this.angle = turn(this.angle, Math.atan2(dy, dx), 4.2 * dt);
          wanted = Math.min(POLICE_SPEED, distance * 3);
        }
      }
      this.speed += clamp(wanted - this.speed, -420 * dt, 300 * dt);
      this.x = clamp(this.x + Math.cos(this.angle) * this.speed * dt, 18, W - 18);
      this.y = clamp(this.y + Math.sin(this.angle) * this.speed * dt, 18, H - 18);
    }

  }

  class Helicopter {

    constructor(x, y) {
      this.name = 'Helicóptero';
      this.variable = 'helicoptero';
      this.kind = 'heli';
      this.x = x;
      this.y = y;
      this.target = null;
      this.flash = 0;
      this.rotor = 0;
    }

    update(car) {
      this.target = { x: car.x, y: car.y };
      this.flash = 0.45;
      beam(this);
      log('sub heli', this.variable + '.update(car) → ilumina (' + car.x + ', ' + car.y + ')');
      hot('heli');
    }

    step(dt) {
      this.flash = Math.max(0, this.flash - dt);
      this.rotor += dt * 22;
      if (this.target) {
        const dx = this.target.x - this.x;
        const dy = this.target.y - this.y;
        const distance = Math.hypot(dx, dy);
        const travel = Math.min(distance, HELI_SPEED * dt);
        if (distance > 1) {
          this.x += dx / distance * travel;
          this.y += dy / distance * travel;
        }
      }
    }

  }

  // ---------- game state ----------

  let state;
  let car;
  let thief;
  let police;
  let helicopter;
  let effects;
  let lastFrame = performance.now();
  const keys = new Set();

  function reset() {
    state = { mode: 'ready', time: 0, notifications: 0, radio: 0, caughtBy: null };
    thief = { x: W / 2, y: H / 2, angle: -Math.PI / 2 };
    car = new StolenCar(thief.x, thief.y);
    police = [];
    helicopter = null;
    effects = { waves: [], beams: [] };
    logView.innerHTML = '';
    addPolice();
    addPolice();
    renderOverlay();
  }

  function addPolice() {
    if (police.length >= MAX_POLICE) {
      return;
    }
    const [x, y] = SPAWNS[police.length];
    const unit = new PoliceCar(police.length + 1, x, y);
    police.push(unit);
    car.attach(unit);
  }

  function addHelicopter() {
    if (helicopter) {
      toggle(helicopter);
      return;
    }
    helicopter = new Helicopter(W / 2, 46);
    car.attach(helicopter);
  }

  function toggle(unit) {
    if (car.observers.includes(unit)) {
      car.detach(unit);
    }
    else {
      car.attach(unit);
    }
  }

  // ---------- callbacks from the subject: log, code and side panel ----------

  function onAttach(observer) {
    log('list', 'car.attach(' + observer.variable + ')');
    hot('attach');
    renderList(observer);
  }

  function onDetach(observer) {
    log('list out', 'car.detach(' + observer.variable + ')');
    hot('detach');
    renderList();
  }

  function onMove(dx, dy) {
    log('', 'car.move(' + signed(dx) + ', ' + signed(dy) + ')');
    hot('move');
  }

  function onNotify(count) {
    state.notifications++;
    effects.waves.push({ x: thief.x, y: thief.y, age: 0 });
    log('', count === 0 ? '→ notifyObservers(): lista vazia, ninguém é avisado'
      : '→ notifyObservers(): avisando ' + count + (count === 1 ? ' observador' : ' observadores'));
    hot('notify');
  }

  function beam(observer) {
    effects.beams.push({ to: observer, age: 0 });
  }

  // ---------- loop ----------

  function frame(now) {
    const dt = Math.min(0.05, (now - lastFrame) / 1000);
    lastFrame = now;
    if (state.mode === 'running') {
      step(dt);
    }
    else {
      police.forEach((unit) => { unit.flash = Math.max(0, unit.flash - dt); });
      if (helicopter) {
        helicopter.rotor += dt * 22;
      }
    }
    stepEffects(dt);
    draw();
    renderHud();
    requestAnimationFrame(frame);
  }

  function step(dt) {
    state.time += dt;

    let ix = 0;
    let iy = 0;
    if (keys.has('ArrowLeft') || keys.has('a')) ix -= 1;
    if (keys.has('ArrowRight') || keys.has('d')) ix += 1;
    if (keys.has('ArrowUp') || keys.has('w')) iy -= 1;
    if (keys.has('ArrowDown') || keys.has('s')) iy += 1;
    if (ix !== 0 || iy !== 0) {
      const length = Math.hypot(ix, iy);
      thief.x = clamp(thief.x + ix / length * THIEF_SPEED * dt, 20, W - 20);
      thief.y = clamp(thief.y + iy / length * THIEF_SPEED * dt, 20, H - 20);
      thief.angle = turn(thief.angle, Math.atan2(iy, ix), 12 * dt);
    }

    state.radio += dt;
    if (state.radio >= RADIO_INTERVAL) {
      state.radio = 0;
      const dx = Math.round(thief.x) - car.x;
      const dy = Math.round(thief.y) - car.y;
      if (dx !== 0 || dy !== 0) {
        car.move(dx, dy);
      }
    }

    police.forEach((unit) => unit.step(dt));
    separate();
    if (helicopter) {
      helicopter.step(dt);
    }

    const catcher = police.find((unit) => Math.hypot(unit.x - thief.x, unit.y - thief.y) < CATCH_DISTANCE);
    if (catcher) {
      state.mode = 'caught';
      state.caughtBy = catcher;
      log('list', catcher.name + ' alcançou o carro roubado');
      renderOverlay();
    }
  }

  function separate() {
    for (let i = 0; i < police.length; i++) {
      for (let j = i + 1; j < police.length; j++) {
        const a = police[i];
        const b = police[j];
        const dx = b.x - a.x;
        const dy = b.y - a.y;
        const distance = Math.hypot(dx, dy) || 1;
        if (distance < 42) {
          const push = (42 - distance) / 2;
          a.x -= dx / distance * push;
          a.y -= dy / distance * push;
          b.x += dx / distance * push;
          b.y += dy / distance * push;
        }
      }
    }
  }

  function stepEffects(dt) {
    effects.waves.forEach((w) => { w.age += dt; });
    effects.beams.forEach((b) => { b.age += dt; });
    effects.waves = effects.waves.filter((w) => w.age < 0.7);
    effects.beams = effects.beams.filter((b) => b.age < 0.45);
  }

  // ---------- drawing ----------

  const background = document.createElement('canvas');

  function paintBackground(scale) {
    background.width = W * scale;
    background.height = H * scale;
    const g = background.getContext('2d');
    g.scale(scale, scale);
    g.fillStyle = '#3a3e42';
    g.fillRect(0, 0, W, H);

    const roadsX = [0, 240, 480, 720, 960];
    const roadsY = [0, 200, 400, 600];
    const half = 34;
    let seed = 7;
    const random = () => {
      seed = (seed * 16807) % 2147483647;
      return seed / 2147483647;
    };
    for (let i = 0; i < roadsX.length - 1; i++) {
      for (let j = 0; j < roadsY.length - 1; j++) {
        const x = roadsX[i] + half;
        const y = roadsY[j] + half;
        const w = roadsX[i + 1] - roadsX[i] - half * 2;
        const h = roadsY[j + 1] - roadsY[j] - half * 2;
        g.fillStyle = '#8d9196';
        roundRect(g, x - 5, y - 5, w + 10, h + 10, 8);
        g.fill();
        g.fillStyle = (i + j) % 3 === 0 ? '#4d5a46' : '#3f5a3c';
        roundRect(g, x, y, w, h, 6);
        g.fill();
        for (let t = 0; t < 7; t++) {
          const tx = x + 14 + random() * (w - 28);
          const ty = y + 14 + random() * (h - 28);
          g.fillStyle = 'rgba(0,0,0,.18)';
          g.beginPath();
          g.arc(tx + 3, ty + 3, 9, 0, Math.PI * 2);
          g.fill();
          g.fillStyle = random() > 0.5 ? '#5f8a4a' : '#557d42';
          g.beginPath();
          g.arc(tx, ty, 9, 0, Math.PI * 2);
          g.fill();
        }
      }
    }

    g.strokeStyle = 'rgba(240, 200, 60, .55)';
    g.lineWidth = 2;
    g.setLineDash([14, 12]);
    roadsX.slice(1, -1).forEach((x) => {
      g.beginPath();
      g.moveTo(x, 0);
      g.lineTo(x, H);
      g.stroke();
    });
    roadsY.slice(1, -1).forEach((y) => {
      g.beginPath();
      g.moveTo(0, y);
      g.lineTo(W, y);
      g.stroke();
    });
    g.setLineDash([]);
  }

  function resize() {
    const scale = Math.max(1, Math.min(2, window.devicePixelRatio || 1));
    canvas.width = W * scale;
    canvas.height = H * scale;
    ctx.setTransform(scale, 0, 0, scale, 0, 0);
    paintBackground(scale);
  }

  function draw() {
    ctx.drawImage(background, 0, 0, W, H);

    if (helicopter && helicopter.target) {
      const gradient = ctx.createRadialGradient(helicopter.x, helicopter.y, 0, helicopter.x, helicopter.y, 70);
      const listening = car.observers.includes(helicopter);
      gradient.addColorStop(0, listening ? 'rgba(255, 236, 150, .45)' : 'rgba(255, 236, 150, .12)');
      gradient.addColorStop(1, 'rgba(255, 236, 150, 0)');
      ctx.fillStyle = gradient;
      ctx.beginPath();
      ctx.arc(helicopter.x, helicopter.y, 70, 0, Math.PI * 2);
      ctx.fill();
    }

    effects.waves.forEach((w) => {
      const t = w.age / 0.7;
      ctx.strokeStyle = 'rgba(255, 120, 100, ' + (0.7 * (1 - t)) + ')';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(w.x, w.y, 18 + t * 70, 0, Math.PI * 2);
      ctx.stroke();
    });

    effects.beams.forEach((b) => {
      const t = Math.min(1, b.age / 0.25);
      const alpha = 1 - b.age / 0.45;
      const color = b.to.kind === 'heli' ? '255, 210, 80' : '120, 180, 255';
      ctx.strokeStyle = 'rgba(' + color + ', ' + (0.55 * alpha) + ')';
      ctx.lineWidth = 2;
      ctx.setLineDash([6, 6]);
      ctx.beginPath();
      ctx.moveTo(thief.x, thief.y);
      ctx.lineTo(b.to.x, b.to.y);
      ctx.stroke();
      ctx.setLineDash([]);
      ctx.fillStyle = 'rgba(' + color + ', ' + alpha + ')';
      ctx.beginPath();
      ctx.arc(thief.x + (b.to.x - thief.x) * t, thief.y + (b.to.y - thief.y) * t, 5, 0, Math.PI * 2);
      ctx.fill();
    });

    police.forEach((unit) => {
      if (unit.target && car.observers.includes(unit)) {
        ctx.strokeStyle = 'rgba(120, 180, 255, .5)';
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.moveTo(unit.target.x - 7, unit.target.y - 7);
        ctx.lineTo(unit.target.x + 7, unit.target.y + 7);
        ctx.moveTo(unit.target.x + 7, unit.target.y - 7);
        ctx.lineTo(unit.target.x - 7, unit.target.y + 7);
        ctx.stroke();
      }
    });

    drawThief();
    police.forEach(drawPolice);
    if (helicopter) {
      drawHelicopter(helicopter);
    }
  }

  function drawCarBody(x, y, angle, body, roof) {
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(angle);
    ctx.fillStyle = 'rgba(0,0,0,.3)';
    roundRect(ctx, -17, -8, 38, 22, 6);
    ctx.fill();
    ctx.fillStyle = body;
    roundRect(ctx, -19, -10, 38, 20, 6);
    ctx.fill();
    ctx.fillStyle = roof;
    roundRect(ctx, -7, -8, 14, 16, 3);
    ctx.fill();
    ctx.fillStyle = 'rgba(190, 220, 240, .85)';
    ctx.fillRect(7, -7, 5, 14);
    ctx.fillStyle = '#ffe9a8';
    ctx.fillRect(17, -8, 2, 4);
    ctx.fillRect(17, 4, 2, 4);
    ctx.restore();
  }

  function drawThief() {
    drawCarBody(thief.x, thief.y, thief.angle, '#d64032', '#8f2219');
    label(thief.x, thief.y + 24, 'carro roubado', '#ffd2cc');
  }

  function drawPolice(unit) {
    const listening = car.observers.includes(unit);
    drawCarBody(unit.x, unit.y, unit.angle, listening ? '#f4f6f8' : '#9aa0a6', listening ? '#1f4f8a' : '#6b7177');
    if (listening) {
      const blink = Math.floor(performance.now() / 160) % 2 === 0;
      ctx.save();
      ctx.translate(unit.x, unit.y);
      ctx.rotate(unit.angle);
      ctx.fillStyle = blink ? '#ff3b30' : '#2f7bff';
      ctx.fillRect(-3, -8, 5, 7);
      ctx.fillStyle = blink ? '#2f7bff' : '#ff3b30';
      ctx.fillRect(-3, 1, 5, 7);
      ctx.restore();
    }
    if (unit.flash > 0) {
      ctx.strokeStyle = 'rgba(120, 180, 255, ' + (unit.flash / 0.45) + ')';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(unit.x, unit.y, 26 + (0.45 - unit.flash) * 30, 0, Math.PI * 2);
      ctx.stroke();
    }
    label(unit.x, unit.y - 22, unit.name + (listening ? '' : ' · fora da escuta'), listening ? '#dbe9ff' : '#c9ccd0');
  }

  function drawHelicopter(heli) {
    const listening = car.observers.includes(heli);
    ctx.save();
    ctx.translate(heli.x, heli.y);
    ctx.fillStyle = 'rgba(0,0,0,.25)';
    ctx.beginPath();
    ctx.ellipse(10, 14, 16, 9, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = listening ? '#e0a800' : '#9aa0a6';
    ctx.fillRect(-2, 0, 4, 26);
    ctx.beginPath();
    ctx.ellipse(0, 0, 11, 15, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = 'rgba(30,30,30,.7)';
    ctx.lineWidth = 3;
    for (let i = 0; i < 2; i++) {
      const a = heli.rotor + i * Math.PI / 2;
      ctx.beginPath();
      ctx.moveTo(Math.cos(a) * -26, Math.sin(a) * -26);
      ctx.lineTo(Math.cos(a) * 26, Math.sin(a) * 26);
      ctx.stroke();
    }
    ctx.restore();
    if (heli.flash > 0) {
      ctx.strokeStyle = 'rgba(255, 210, 80, ' + (heli.flash / 0.45) + ')';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(heli.x, heli.y, 30 + (0.45 - heli.flash) * 30, 0, Math.PI * 2);
      ctx.stroke();
    }
    label(heli.x, heli.y - 32, 'Helicóptero' + (listening ? '' : ' · fora da escuta'), listening ? '#ffe9a8' : '#c9ccd0');
  }

  function label(x, y, text, color, size = 12) {
    ctx.font = '600 ' + size + 'px "Segoe UI", Arial, sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    const width = ctx.measureText(text).width + 10;
    ctx.fillStyle = 'rgba(20, 22, 24, .65)';
    roundRect(ctx, x - width / 2, y - size / 2 - 3, width, size + 6, 4);
    ctx.fill();
    ctx.fillStyle = color;
    ctx.fillText(text, x, y + 0.5);
  }

  function roundRect(g, x, y, w, h, r) {
    g.beginPath();
    g.moveTo(x + r, y);
    g.arcTo(x + w, y, x + w, y + h, r);
    g.arcTo(x + w, y + h, x, y + h, r);
    g.arcTo(x, y + h, x, y, r);
    g.arcTo(x, y, x + w, y, r);
    g.closePath();
  }

  // ---------- side panel ----------

  function renderList(added) {
    const items = car.observers.map((o) =>
      '<span class="item' + (o.kind === 'heli' ? ' heli' : '') + (o === added ? ' new' : '') + '">' + o.variable + '</span>');
    listView.innerHTML = '<span class="k">this</span>.observers = [' + items.join(', ') + ']';

    const units = police.concat(helicopter ? [helicopter] : []);
    unitButtons.innerHTML = units.map((u, i) => {
      const on = car.observers.includes(u);
      const key = u.kind === 'heli' ? 'H' : String(u.number);
      return '<button type="button" class="chase-unit' + (u.kind === 'heli' ? ' heli' : '') + (on ? ' on' : '') +
        '" data-unit="' + i + '">' + u.name + ' <span class="off">' + (on ? 'na escuta' : 'fora') + ' · ' + key + '</span></button>';
    }).join('');
    unitButtons.querySelectorAll('button').forEach((button) => {
      button.addEventListener('click', () => {
        toggle(units[Number(button.dataset.unit)]);
        canvas.focus();
      });
    });

    addPoliceButton.disabled = police.length >= MAX_POLICE;
    heliButton.disabled = helicopter !== null;
  }

  function log(kind, text) {
    const item = document.createElement('li');
    item.className = kind;
    item.innerHTML = '<span class="time">' + state.time.toFixed(1).replace('.', ',') + 's</span>' + escapeHtml(text);
    item.title = text;
    logView.prepend(item);
    while (logView.children.length > 7) {
      logView.lastElementChild.remove();
    }
  }

  const KEYWORDS = new Set(['interface', 'static', 'class', 'private', 'final', 'new', 'void', 'int', 'this', 'for', 'public', 'implements']);

  function renderCode() {
    const html = SOURCE.map(([n, text], i) => {
      const gap = i > 0 && n - SOURCE[i - 1][0] > 2;
      return (gap ? '<span class="chase-line gap"><span class="ln">⋮</span></span>' : '') +
        '<span class="chase-line" data-line="' + n + '"><span class="ln">' + n + '</span>' + highlight(text) + '</span>';
    }).join('');
    const pre = document.createElement('pre');
    pre.innerHTML = html;
    document.getElementById('chase-code').appendChild(pre);
  }

  function highlight(line) {
    return escapeHtml(line).replace(/(&quot;.*?&quot;|"[^"]*")|(@\w+)|\b([A-Za-z_]\w*)\b/g, (m, str, ann, word) => {
      if (str) return '<span class="s">' + m + '</span>';
      if (ann) return '<span class="a">' + m + '</span>';
      if (KEYWORDS.has(word)) return '<span class="k">' + m + '</span>';
      if (/^[A-Z]/.test(word)) return '<span class="t">' + m + '</span>';
      return m;
    });
  }

  const hotTimers = new Map();

  function hot(group) {
    LINES[group].forEach((n) => {
      const line = document.querySelector('.chase-line[data-line="' + n + '"]');
      if (!line) {
        return;
      }
      line.classList.add('hot');
      clearTimeout(hotTimers.get(n));
      hotTimers.set(n, setTimeout(() => line.classList.remove('hot'), 380));
    });
    keepVisible(document.querySelector('.chase-line[data-line="' + LINES[group][0] + '"]'));
  }

  // Scrolls only the code box (never the page) when the executing line is out of view
  function keepVisible(line) {
    const box = line && line.parentElement;
    if (!box || box.scrollHeight <= box.clientHeight) {
      return;
    }
    const top = line.offsetTop - box.offsetTop;
    if (top < box.scrollTop || top + line.offsetHeight > box.scrollTop + box.clientHeight) {
      box.scrollTop = Math.max(0, top - box.clientHeight / 3);
    }
  }

  function renderHud() {
    hud.notifications.textContent = state.notifications;
    hud.observers.textContent = car.observers.length;
    hud.time.textContent = state.time.toFixed(1).replace('.', ',') + ' s';
  }

  function renderOverlay() {
    const messages = {
      ready: ['Aperte uma seta para fugir', 'Se as setas não responderem, clique no mapa primeiro.'],
      paused: ['Pausado', 'Espaço continua · R reinicia'],
      caught: [state.caughtBy ? state.caughtBy.name + ' alcançou o carro!' : '',
        'Fuga de ' + state.time.toFixed(1).replace('.', ',') + ' s · ' + state.notifications + ' notificações · R reinicia']
    };
    const message = messages[state.mode];
    overlay.hidden = !message;
    if (message) {
      overlay.innerHTML = '<strong>' + escapeHtml(message[0]) + '</strong><span>' + escapeHtml(message[1]) + '</span>';
    }
  }

  // ---------- input ----------

  const MOVE_KEYS = ['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'a', 'd', 'w', 's'];

  window.addEventListener('keydown', (event) => {
    if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement || event.ctrlKey || event.metaKey || event.altKey) {
      return;
    }
    const key = event.key.length === 1 ? event.key.toLowerCase() : event.key;
    if (MOVE_KEYS.includes(key)) {
      event.preventDefault();
      keys.add(key);
      if (state.mode === 'ready') {
        state.mode = 'running';
        renderOverlay();
      }
    }
    else if (key === ' ') {
      event.preventDefault();
      if (state.mode === 'running' || state.mode === 'paused') {
        state.mode = state.mode === 'running' ? 'paused' : 'running';
        renderOverlay();
      }
    }
    else if (key === 'r') {
      reset();
    }
    else if (key === 'h') {
      addHelicopter();
    }
    else if (/^[1-5]$/.test(key)) {
      const unit = police[Number(key) - 1];
      if (unit) {
        toggle(unit);
      }
    }
  });

  window.addEventListener('keyup', (event) => {
    keys.delete(event.key.length === 1 ? event.key.toLowerCase() : event.key);
  });

  window.addEventListener('blur', () => keys.clear());

  canvas.addEventListener('click', (event) => {
    const rect = canvas.getBoundingClientRect();
    const x = (event.clientX - rect.left) * W / rect.width;
    const y = (event.clientY - rect.top) * H / rect.height;
    const units = police.concat(helicopter ? [helicopter] : []);
    const hit = units.find((u) => Math.hypot(u.x - x, u.y - y) < 30);
    if (hit) {
      toggle(hit);
    }
    canvas.focus();
  });

  addPoliceButton.addEventListener('click', () => {
    addPolice();
    canvas.focus();
  });

  heliButton.addEventListener('click', () => {
    addHelicopter();
    canvas.focus();
  });

  document.getElementById('restart').addEventListener('click', () => {
    reset();
    canvas.focus();
  });

  window.addEventListener('resize', resize);

  // ---------- helpers ----------

  function turn(from, to, maxStep) {
    let delta = Math.atan2(Math.sin(to - from), Math.cos(to - from));
    delta = clamp(delta, -maxStep, maxStep);
    return from + delta;
  }

  function clamp(value, min, max) {
    return Math.max(min, Math.min(max, value));
  }

  function signed(value) {
    return (value > 0 ? '+' : '') + value;
  }

  function escapeHtml(text) {
    return String(text).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  }

  renderCode();
  resize();
  reset();
  canvas.focus({ preventScroll: true });
  requestAnimationFrame(frame);
})();
