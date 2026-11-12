function cyrb53(str, seed) {
	seed ??= 0;
	
	let h1 = 0xdeadbeef ^ seed;
	let h2 = 0x41c6ce57 ^ seed;

	for (let i = 0, ch; i < str.length; i++) {
		ch = str.charCodeAt(i);
		h1 = Math.imul(h1 ^ ch, 2654435761);
		h2 = Math.imul(h2 ^ ch, 1597334677);
	}

	h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507);
	h1 ^= Math.imul(h2 ^ (h2 >>> 13), 3266489909);
	h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507);
	h2 ^= Math.imul(h1 ^ (h1 >>> 13), 3266489909);

	return 4294967296 * (2097151 & h2) + (h1 >>> 0);
};

function getExtensionId() {
	return chrome?.runtime?.id || '';
}

function setItem(key, value) {
	return localStorage.setItem(key, value);
}
function getItem(key) {
	return localStorage.getItem(key);
}
function removeItem(key) {
	return localStorage.removeItem(key);
}

const CHARSET = [
	'BDFGHJKLMNPRSTVWYZ',
	'aeiou',
	'bdfgkmnprstvxyz',
	'0123456789',
	'0123456789',
];
const CSIZES = CHARSET.map(c => c.length);
const GROUP_MAX = CSIZES.reduce((a, b) => a * b, 1);

async function hashMainKey(m) {
	const mbin = new TextEncoder().encode(m).reduce((acc, item)=>(acc + item.toString(2).padStart(8,'0')), '');
	const buffer = new TextEncoder().encode(mbin);
	const bytes = new Uint8Array(await crypto.subtle.digest('SHA-256', buffer));
	return Array.from(bytes).map(b => b.toString(16).padStart(2, '0')).join('');
}

function normalizeServiceKey(s) {
	return s.normalize('NFC').toLowerCase();
}

function hashServiceKey(sn) {
	let ext = getExtensionId().slice(0,8);
	let seed = 0;
	for(let i=0; i<ext.length; ++i) {
		seed = (seed << 4) | ((ext.charCodeAt(i)-97)&0xf);
	}
	
	return cyrb53(sn, seed).toString(36).padStart(11, '0');
}

async function generateRawPassword(mh, sn) {
	const buffer = new TextEncoder().encode(mh + sn);
	const bytes = new Uint8Array(await crypto.subtle.digest('SHA-256', buffer));
	return encodeHashByteArray(bytes);
}

function encodeHashByteArray(bytes) {
	const flag = bytes[0] >> 7;
	
	let result = '';
	let value = 0;
	let countBits = 0;
	for (let i = bytes.length - 1; i >= 0; i--) {
		value |= bytes[i] << countBits;
		countBits += 8;
		if(countBits >= 17) {
			let v = value & 0x1ffff;
			result = encodeChunk((result.length+flag)%2 ? GROUP_MAX-1-v : v) + result;
			countBits -= 17;
			value >>= 17;
		}
	}
	return result;
}

function encodeChunk(v) {
	let result = '';
	for (let p = CHARSET.length - 1; p >= 0; p--) {
		result = CHARSET[p][v % CSIZES[p]] + result;
		v = Math.floor(v / CSIZES[p]);
	}
	return result;
}

function wrapPassword(raw, sym, len) {
	if(sym.length > 0) {
		raw = raw.slice(0, 5) + sym[0] + raw.slice(5);
	}
	return raw.slice(0, Math.max(0, len));
}

function wrapArgKey(args) {
	return args.sn.length.toString()+':'+args.sn + args.mh + args.len.toString().padStart(2,'0') + '|' + args.sym;
}

const elMainKey = document.getElementById('mainKey');
const elSaveMainKey = document.getElementById('isSaveMainKey');
const elServiceKey = document.getElementById('serviceKey');
const elSymbol = document.getElementById('symbol');
const elLengthRange = document.getElementById('lengthRange');
const elLength = document.getElementById('length');
const elOutput = document.getElementById('output');
const elCopiedMsg = document.getElementById('copiedMsg');

let fallback = { l:20, };
let state = {
	mh:null,
	sn:'',
	sym:'',
	len:20,
}

let rawCaches = {};

let checkingTimer = null;
let isPendingCheck = false;

let pendingCopyArg = null;

function updateLength(len) {
	state.len = len;

	elLength.value = len;
	elLengthRange.value = len;
}

function hideCopyMessage() {
	elCopiedMsg.textContent = '';
}

function showCopyMessage() {
	elCopiedMsg.textContent = 'Password copied!';
}

function startCheckingTimer() {
	if(checkingTimer) {
		isPendingCheck = true;
		return;
	}
	checkingTimer = setTimeout(() => {
		if(elOutput.argKey != wrapArgKey(state)) {
			elOutput.value = '';
		}
		checkingTimer = null;
		
		if(isPendingCheck) {
			isPendingCheck = false;
			startCheckingTimer();
		}
	}, 250);
}

function updatePassword() {
	hideCopyMessage();
	
	let {mh, sn, sym, len} = state;
	let raw = rawCaches[mh+sn];
	if(!raw) return;
	
	elOutput.value = wrapPassword(raw, sym, len);
	elOutput.argKey = wrapArgKey({mh, sn, sym, len});
	
	if(pendingCopyArg && pendingCopyArg.mh+pendingCopyArg.sn == mh+sn) {
		copyPassword(pendingCopyArg);
	}
}

async function startPasswordGeneration() {
	startCheckingTimer();
	
	let {mh, sn, sym, len} = state;
	let raw = rawCaches[mh+sn] ?? await generateRawPassword(mh, sn);
	rawCaches[mh+sn] = raw;
	
	updatePassword();
}

function copyPassword(args) {
	let raw = rawCaches[args.mh+args.sn];
	if(!raw) {
		pendingCopyArg = {
			mh: args.mh,
			sn: args.sn,
			sym: args.sym,
			len: args.len,
		};
		return;
	}

	navigator.clipboard.writeText(elOutput.value).then(() => {
		showCopyMessage();
		pendingCopyArg = null;

		let {sn, len} = args;
		let sh = hashServiceKey(sn);
		setItem('sc:' + sh, JSON.stringify({ l:len, }));
	});
}

function onInputKeydown(ev) {
	if(ev.key !== 'Enter') return;
	copyPassword(state);
}

function onOutputClick() {
	copyPassword(state);
}

function onLengthChange(ev) {
	let len = parseInt(ev.target.value);
	if( !(len >= 6) ) len = 6;
	if(len > 75) len = 75;

	updateLength(len);
	fallback.l = len;

	startPasswordGeneration();
}

elMainKey.addEventListener('keydown', onInputKeydown);
elServiceKey.addEventListener('keydown', onInputKeydown);
elSymbol.addEventListener('keydown', onInputKeydown);
elOutput.addEventListener('click', onOutputClick);

elMainKey.addEventListener('input', async () => {
	state.mh = await hashMainKey(elMainKey.value);
	startPasswordGeneration();
	
	if(elSaveMainKey.checked) {
		setItem('mainHash', state.mh);
	};
});

elSaveMainKey.addEventListener('change', () => {
	let isSave = elSaveMainKey.checked;
	setItem('isSaveMainHash', isSave);
	if(!isSave) {
		removeItem('mainHash');
		elOutput.value = '';
		elMainKey.placeholder = '';
	} else {
		elMainKey.placeholder = 'Change Saved Main Key';
		setItem('mainHash', state.mh);
	}
});

elServiceKey.addEventListener('input', () => {
	state.sn = normalizeServiceKey(elServiceKey.value);
	let sh = hashServiceKey(state.sn);

	let item = getItem('sc:' + sh);
	let config = item ? JSON.parse(item) : { l:fallback.l, };

	updateLength(config.l);
	
	startPasswordGeneration();
});

elSymbol.addEventListener('input', () => {
	state.sym = elSymbol.value;
	startPasswordGeneration();
});

elLength.addEventListener('change', onLengthChange);
elLengthRange.addEventListener('input', onLengthChange);

(async ()=>{
	let isSave = getItem('isSaveMainHash');
	
	if(isSave != 'true') {
		state.mh = await hashMainKey(elMainKey.value);
		return;
	}
	state.mh = getItem('mainHash');
	elSaveMainKey.checked = true;
	elMainKey.placeholder = 'Change Saved Main Key';
	elServiceKey.focus();
})()