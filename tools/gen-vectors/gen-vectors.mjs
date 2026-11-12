// Generates core-lik31/src/test/resources/golden-vectors.json by running the
// browser extension's own functions (tools/gen-vectors/popup.js, unmodified).
// Usage: node tools/gen-vectors/gen-vectors.mjs
import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import vm from 'node:vm';

const here = dirname(fileURLToPath(import.meta.url));
const source = readFileSync(join(here, 'popup.js'), 'utf8');

// Everything before the first DOM lookup is pure algorithm code.
const marker = 'const elMainKey';
const cut = source.indexOf(marker);
if (cut < 0) throw new Error(`popup.js layout changed: "${marker}" not found`);

const sandbox = { TextEncoder, crypto: globalThis.crypto, Math, chrome: undefined };
vm.createContext(sandbox);
vm.runInContext(
	source.slice(0, cut) +
		'\nthis.api = { hashMainKey, normalizeServiceKey, generateRawPassword, wrapPassword };',
	sandbox,
);
const { hashMainKey, normalizeServiceKey, generateRawPassword, wrapPassword } = sandbox.api;

// [mainKey, serviceKey, symbol, length]
const handPicked = [
	['correct horse', 'github.com', '', 20],
	['correct horse', 'github.com', '!', 20],
	['correct horse', 'GitHub.com', '', 20],
	['correct horse', 'github.com', '', 16],
	['correct horse', 'github.com', '!', 16],
	['correct horse', 'github.com', '', 6],
	['correct horse', 'github.com', '!', 6],
	['correct horse', 'github.com', '', 75],
	['correct horse', 'github.com', '#', 75],
	['correct horse', 'google.com', '', 20],
	['correct horse', 'google.com', '@', 14],
	// empty inputs
	['', 'github.com', '', 20],
	['correct horse', '', '', 20],
	['', '', '', 20],
	// whitespace is significant
	['correct horse', 'github.com ', '', 20],
	['correct horse', ' github.com', '', 20],
	['correct horse ', 'github.com', '', 20],
	// symbol variants: space, letter, multi-char (only first code unit is used)
	['correct horse', 'github.com', ' ', 20],
	['correct horse', 'github.com', 'x', 20],
	['correct horse', 'github.com', '!?', 20],
	// CJK
	['主密鑰測試', '銀行', '', 20],
	['主密鑰測試', '銀行', '＠', 24],
	// main key is NOT normalized: precomposed vs combining differ
	['café', 'x', '', 20],
	['café', 'x', '', 20],
	// service key IS NFC-normalized: precomposed vs combining agree
	['k', 'café', '', 20],
	['k', 'café', '', 20],
	// lowercasing special cases
	['k', 'STRASSE', '', 20],
	['k', 'straße', '', 20],
	['k', 'STRAẞE', '', 20],
	['k', 'İstanbul', '', 20],
	['k', 'IRMAK', '', 20],
	['k', 'ΟΔΟΣ', '', 20],
	['k', 'Σ', '', 20],
	['k', 'ΑΣ ΑΣ', '', 20],
	['k', 'Ｇｏｏｇｌｅ', '', 20],
	['k', 'ǅ', '', 20],
	['k', 'Ⅻ', '', 20],
	['k', '𐐀', '', 20],
	// astral characters and surrogates
	['pass😀word', 'x', '', 20],
	['k', 'emoji😀site', '', 20],
	['k', 'x', '😀', 20],
	['k\ud800', 'x', '', 20],
	['\udc00k', 'x', '', 20],
	['k', 'x\ud83d', '', 20],
	// case of the main key matters
	['Correct Horse', 'github.com', '', 20],
	// long inputs
	['m'.repeat(300), 's'.repeat(300), '-', 40],
];

// Deterministic pseudo-random cases (mulberry32) for breadth.
function mulberry32(a) {
	return () => {
		a |= 0; a = (a + 0x6d2b79f5) | 0;
		let t = Math.imul(a ^ (a >>> 15), 1 | a);
		t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
		return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
	};
}
const rnd = mulberry32(0x4c696b33);
const pools = [
	'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789',
	' .-_/@:!?#$%&*+=~',
	'ÀÉÎÕÜßçñøåĞİıŞŁŻŒÆ',
	'ΑΒΓΔΣΩαβγδσςω',
	'АБВГДЖЯабвгджя',
	'密碼產生器銀行測試東京かなカナ한글',
	'̧̣́̈',
	'😀🔑👍𝒳𐐀',
];
function randomString(maxLen) {
	const len = Math.floor(rnd() * (maxLen + 1));
	let out = '';
	for (let i = 0; i < len; i++) {
		const pool = Array.from(pools[rnd() < 0.6 ? 0 : Math.floor(rnd() * pools.length)]);
		out += pool[Math.floor(rnd() * pool.length)];
	}
	return out;
}
const symbols = ['', '', '!', '@', '#', '-', '_', '.', '$', '*'];
const random = [];
for (let i = 0; i < 200; i++) {
	random.push([
		randomString(24),
		randomString(24),
		symbols[Math.floor(rnd() * symbols.length)],
		6 + Math.floor(rnd() * 70),
	]);
}

const out = [];
for (const [m, s, sym, len] of [...handPicked, ...random]) {
	const mh = await hashMainKey(m);
	const sn = normalizeServiceKey(s);
	const raw = await generateRawPassword(mh, sn);
	out.push({ m, s, sym, len, mh, sn, raw, pw: wrapPassword(raw, sym, len) });
}

const target = join(here, '..', '..', 'core-lik31', 'src', 'test', 'resources', 'golden-vectors.json');
// JSON.stringify escapes lone surrogates as \uXXXX, so the file is valid UTF-8.
writeFileSync(target, JSON.stringify(out, null, 1) + '\n');
console.log(`wrote ${out.length} vectors to ${target}`);
