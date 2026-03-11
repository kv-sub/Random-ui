#!/usr/bin/env node
/**
 * Simple HTTP server to view JaCoCo coverage reports in Codespace.
 *
 * Usage:
 *   node serve-jacoco.js
 *
 * Then open: http://localhost:8081 (or forward the port in Codespace)
 *
 * Generate reports first with:
 *   cd insurance-claim-system && mvn clean test jacoco:report
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 8081;
const REPORT_DIR = path.join(__dirname, 'target', 'site');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css',
  '.js': 'application/javascript',
  '.png': 'image/png',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.xml': 'application/xml',
  '.csv': 'text/csv',
};

function serveDirectory(dirPath, urlPath, res) {
  let entries;
  try {
    entries = fs.readdirSync(dirPath, { withFileTypes: true });
  } catch {
    res.writeHead(500, { 'Content-Type': 'text/plain' });
    res.end('Error reading directory');
    return;
  }

  const links = entries
    .sort((a, b) => {
      if (a.isDirectory() !== b.isDirectory()) return a.isDirectory() ? -1 : 1;
      return a.name.localeCompare(b.name);
    })
    .map((e) => {
      const href = path.posix.join(urlPath, e.name) + (e.isDirectory() ? '/' : '');
      const icon = e.isDirectory() ? '📁' : '📄';
      const label = `${icon} ${e.name}${e.isDirectory() ? '/' : ''}`;
      return `<li><a href="${href}">${label}</a></li>`;
    })
    .join('\n');

  const parentLink = urlPath !== '/' ? `<p><a href="..">⬆ Parent directory</a></p>` : '';

  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>JaCoCo Reports — ${urlPath}</title>
  <style>
    body { font-family: sans-serif; max-width: 900px; margin: 40px auto; padding: 0 20px; }
    h1 { color: #333; }
    a { color: #0066cc; text-decoration: none; }
    a:hover { text-decoration: underline; }
    li { padding: 5px 0; font-size: 1.05em; }
    code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; }
  </style>
</head>
<body>
  <h1>JaCoCo Coverage Reports</h1>
  <p>Path: <code>${urlPath}</code></p>
  ${parentLink}
  <ul>${links}</ul>
</body>
</html>`);
}

const server = http.createServer((req, res) => {
  const rawPath = req.url.split('?')[0];
  let urlPath;
  try {
    urlPath = decodeURIComponent(rawPath);
  } catch {
    res.writeHead(400, { 'Content-Type': 'text/plain' });
    res.end('Bad request');
    return;
  }

  // Prevent path traversal
  const fsPath = path.normalize(path.join(REPORT_DIR, urlPath));
  if (!fsPath.startsWith(REPORT_DIR)) {
    res.writeHead(403, { 'Content-Type': 'text/plain' });
    res.end('Forbidden');
    return;
  }

  if (!fs.existsSync(fsPath)) {
    res.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(`<!DOCTYPE html>
<html><body>
<h2>Not Found</h2>
<p>Path <code>${urlPath}</code> does not exist in report directory.</p>
<p>Generate reports first: <code>mvn clean test jacoco:report</code></p>
<p><a href="/">Go to root</a></p>
</body></html>`);
    return;
  }

  const stat = fs.statSync(fsPath);

  if (stat.isDirectory()) {
    const indexPath = path.join(fsPath, 'index.html');
    if (fs.existsSync(indexPath)) {
      const content = fs.readFileSync(indexPath);
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(content);
    } else {
      serveDirectory(fsPath, urlPath === '' ? '/' : urlPath, res);
    }
    return;
  }

  const ext = path.extname(fsPath).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';
  const content = fs.readFileSync(fsPath);
  res.writeHead(200, { 'Content-Type': contentType });
  res.end(content);
});

server.listen(PORT, () => {
  console.log('\n╔══════════════════════════════════════════════════════╗');
  console.log(`║  JaCoCo Report Server running on port ${PORT}           ║`);
  console.log('╚══════════════════════════════════════════════════════╝');
  console.log(`\n  Local:     http://localhost:${PORT}`);
  console.log(`  Reports:   ${REPORT_DIR}`);
  console.log('\n  Available reports:');
  console.log('    /jacoco/          Unit test coverage');
  console.log('    /jacoco-it/       Integration test coverage');
  console.log('    /jacoco-merged/   Combined (unit + integration) coverage ← recommended');
  console.log('\n  In Codespace: forward port ' + PORT + ' to view in browser.');
  console.log('\n  Generate reports first (from insurance-claim-system/):');
  console.log('    mvn clean verify jacoco:report\n');
});
