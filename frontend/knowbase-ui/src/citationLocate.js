/**
 * Citation locate helpers for Excel cell highlighting and Word snippet scroll/highlight.
 */

export function hasCellCoordinates(metadata) {
  return Array.isArray(metadata?.cellCoordinates) && metadata.cellCoordinates.length > 0;
}

export function primaryCellRef(metadata) {
  if (!metadata || typeof metadata !== 'object') {
    return null;
  }
  if (metadata.primaryCellRef) {
    return String(metadata.primaryCellRef);
  }
  if (Array.isArray(metadata.cellRefs) && metadata.cellRefs.length) {
    return String(metadata.cellRefs[0]);
  }
  if (hasCellCoordinates(metadata)) {
    const first = metadata.cellCoordinates.find((cell) => cell?.cellRef || cell?.coordinate);
    if (first?.cellRef) {
      return String(first.cellRef);
    }
    if (first?.coordinate) {
      return String(first.coordinate);
    }
  }
  return null;
}

export function parseCitationLocateTargets(metadata) {
  if (!metadata || typeof metadata !== 'object') {
    return [];
  }
  const targets = [];
  if (hasCellCoordinates(metadata)) {
    for (const cell of metadata.cellCoordinates) {
      if (!cell || typeof cell !== 'object') {
        continue;
      }
      const rowIndex = Number(cell.rowIndex);
      const columnIndex = Number(cell.columnIndex);
      if (!Number.isFinite(rowIndex) || !Number.isFinite(columnIndex)) {
        continue;
      }
      targets.push({
        rowIndex,
        columnIndex,
        rowSpan: Math.max(1, Number(cell.rowSpan) || 1),
        columnSpan: Math.max(1, Number(cell.columnSpan) || 1),
        cellRef: cell.cellRef || cell.coordinate || null
      });
    }
    if (targets.length) {
      return targets;
    }
  }
  if (metadata.rowIndex == null) {
    return targets;
  }
  const rowIndex = Number(metadata.rowIndex);
  if (!Number.isFinite(rowIndex)) {
    return targets;
  }
  const colStart = metadata.columnStart != null ? Number(metadata.columnStart) : 0;
  const colEnd = metadata.columnEnd != null ? Number(metadata.columnEnd) : colStart;
  for (let columnIndex = colStart; columnIndex <= colEnd; columnIndex += 1) {
    targets.push({
      rowIndex,
      columnIndex,
      rowSpan: 1,
      columnSpan: 1,
      cellRef: null
    });
  }
  return targets;
}

export function clearExcelHighlights(container) {
  if (!container) {
    return;
  }
  container.querySelectorAll('.citation-cell-highlight').forEach((cell) => {
    cell.classList.remove('citation-cell-highlight');
  });
}

export function highlightExcelCells(container, metadata) {
  clearExcelHighlights(container);
  const targets = parseCitationLocateTargets(metadata);
  if (!container || !targets.length) {
    return [];
  }
  const table = container.querySelector('table');
  if (!table) {
    return [];
  }
  const highlighted = [];
  for (const target of targets) {
    for (let rowOffset = 0; rowOffset < target.rowSpan; rowOffset += 1) {
      const row = table.rows[target.rowIndex + rowOffset];
      if (!row) {
        continue;
      }
      for (let colOffset = 0; colOffset < target.columnSpan; colOffset += 1) {
        const cell = row.cells[target.columnIndex + colOffset];
        if (cell) {
          cell.classList.add('citation-cell-highlight');
          highlighted.push(cell);
        }
      }
    }
  }
  if (highlighted[0]) {
    highlighted[0].scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });
  }
  return highlighted;
}

export function clearDocxHighlights(container) {
  if (!container) {
    return;
  }
  container.querySelectorAll('mark.docx-citation-highlight').forEach((mark) => {
    const parent = mark.parentNode;
    if (!parent) {
      return;
    }
    parent.replaceChild(document.createTextNode(mark.textContent || ''), mark);
    parent.normalize();
  });
}

export function highlightDocxSnippet(container, content) {
  clearDocxHighlights(container);
  if (!container || !content) {
    return false;
  }
  const snippet = String(content).trim().slice(0, 48);
  if (!snippet) {
    return false;
  }
  const needle = snippet.slice(0, Math.min(snippet.length, 24));
  const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT);
  let node = walker.nextNode();
  while (node) {
    const text = node.textContent || '';
    const index = text.indexOf(needle);
    if (index >= 0) {
      const range = document.createRange();
      range.setStart(node, index);
      range.setEnd(node, Math.min(text.length, index + snippet.length));
      const mark = document.createElement('mark');
      mark.className = 'docx-citation-highlight';
      try {
        range.surroundContents(mark);
        mark.scrollIntoView({ behavior: 'smooth', block: 'center' });
        return true;
      } catch {
        range.startContainer.parentElement?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        return false;
      }
    }
    node = walker.nextNode();
  }
  return false;
}

export function canLocateChunk(metadata) {
  if (!metadata || typeof metadata !== 'object') {
    return false;
  }
  return metadata.pageNumber != null
    || (metadata.sheetName != null && (metadata.rowIndex != null || hasCellCoordinates(metadata)))
    || metadata.bbox != null
    || metadata.wordSectionPath != null
    || metadata.tableRegionLabel != null;
}
