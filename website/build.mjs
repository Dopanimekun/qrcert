import fs from "node:fs";
import path from "node:path";

// ── Configuração ────────────────────────────────────────────────────────────

const ROOT = new URL(".", import.meta.url).pathname;
const KEYS_PATH = path.join(ROOT, "keys.js");
const DIST = path.join(ROOT, "dist");

const PACKAGES = [
  {
    name: "raiz",
    inject: [],
    copy: ["index.html"],
  },
  {
    name: "emissor",
    inject: ["emissor/emissor.html"],
    copy: ["emissor/demo-keys.js"],
  },
  {
    name: "verificador-pwa",
    inject: ["verificador-pwa/verificador.html"],
    copy: [],
  },
];

const KEYS_SCRIPT_PATTERNS = [
  /<script\s+src=["']keys\.js["']\s*><\/script>/gi,
  /<script\s+src=["']\.\.\/keys\.js["']\s*><\/script>/gi,
  /<script\s+src=["']\.\/keys\.js["']\s*><\/script>/gi,
];

// ── Helpers ─────────────────────────────────────────────────────────────────

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

function readFile(filePath) {
  const full = path.join(ROOT, filePath);
  if (!fs.existsSync(full)) {
    throw new Error(`Arquivo não encontrado: ${full}`);
  }
  return fs.readFileSync(full, "utf8");
}

function writeFile(filePath, content) {
  const full = path.join(DIST, filePath);
  ensureDir(path.dirname(full));
  fs.writeFileSync(full, content, "utf8");
}

function copyFile(filePath) {
  const src = path.join(ROOT, filePath);
  const dest = path.join(DIST, filePath);
  if (!fs.existsSync(src)) {
    console.warn(`  ⚠  Arquivo para copiar não encontrado, pulando: ${src}`);
    return;
  }
  ensureDir(path.dirname(dest));
  fs.copyFileSync(src, dest);
}

// ── Injeção ─────────────────────────────────────────────────────────────────

function injectKeys(htmlContent, keysContent) {
  // Bloco inline que substitui o <script src="keys.js">
  const inlineBlock = `<script>\n/* keys.js — injetado pelo build.mjs */\n${keysContent}\n</script>`;

  let result = htmlContent;
  let replaced = false;

  for (const pattern of KEYS_SCRIPT_PATTERNS) {
    const before = result;
    result = result.replace(pattern, inlineBlock);
    if (result !== before) {
      replaced = true;
      break; // só substitui a primeira ocorrência encontrada
    }
  }

  if (!replaced) {
    // Avisa mas não falha — pode ser que o HTML já não use keys.js
    console.warn(
      '  ⚠  Nenhum <script src="keys.js"> encontrado neste HTML. ' +
        "Verifique se o caminho bate com os padrões em KEYS_SCRIPT_PATTERNS.",
    );
  }

  return result;
}

// ── Build principal ──────────────────────────────────────────────────────────

function build() {
  const startTime = Date.now();
  console.log("\n🔨 build.mjs — iniciando build\n");

  // Lê keys.js uma única vez
  if (!fs.existsSync(KEYS_PATH)) {
    console.error(`❌  keys.js não encontrado em: ${KEYS_PATH}`);
    process.exit(1);
  }
  const keysContent = fs.readFileSync(KEYS_PATH, "utf8");
  console.log(`✔  keys.js lido (${keysContent.length} chars)\n`);

  for (const pkg of PACKAGES) {
    console.log(`📦 Pacote: ${pkg.name}`);

    // Injeta keys.js em cada HTML do pacote
    for (const htmlRelPath of pkg.inject) {
      try {
        const html = readFile(htmlRelPath);
        const injected = injectKeys(html, keysContent);
        writeFile(htmlRelPath, injected);
        console.log(`  ✔  injetado → dist/${htmlRelPath}`);
      } catch (e) {
        console.error(`  ❌  Erro em ${htmlRelPath}: ${e.message}`);
      }
    }

    // Copia demais arquivos sem alteração
    for (const filePath of pkg.copy) {
      copyFile(filePath);
      console.log(`  ✔  copiado  → dist/${filePath}`);
    }

    console.log();
  }

  const elapsed = Date.now() - startTime;
  console.log(`✅ Build concluído em ${elapsed}ms → dist/\n`);
}

// ── Watch mode ───────────────────────────────────────────────────────────────

const isWatch = process.argv.includes("--watch");

if (isWatch) {
  // Coleta todos os arquivos monitorados
  const watchTargets = [
    KEYS_PATH,
    ...PACKAGES.flatMap((p) => [
      ...p.inject.map((f) => path.join(ROOT, f)),
      ...p.copy.map((f) => path.join(ROOT, f)),
    ]),
  ].filter((f) => fs.existsSync(f));

  build(); // executa imediatamente

  console.log("👁  Watch mode ativo. Aguardando mudanças...\n");

  let debounce = null;
  for (const target of watchTargets) {
    fs.watch(target, () => {
      clearTimeout(debounce);
      debounce = setTimeout(() => {
        console.log(`♻  Mudança detectada em: ${path.relative(ROOT, target)}`);
        build();
      }, 150);
    });
  }
} else {
  build();
}
