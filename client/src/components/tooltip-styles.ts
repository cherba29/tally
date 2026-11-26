import {css} from 'lit';

/**
 * Shared visual styles for all floating popups (statement, account, summary,
 * transfers summary, and recurring transactions) so they share a consistent
 * color scheme, header bar, close button, and table formatting.
 */
export const tooltipSharedStyles = css`
  :host {
    display: block;
    color: #1f2328;
  }
  .header {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 4px;
  }
  .close-btn {
    padding: 0 5px;
    font-size: 12px;
    line-height: 15px;
    font-weight: 700;
    border: 1px solid #8c959f;
    border-radius: 3px;
    background-color: #fff;
    color: #1f2328;
    cursor: pointer;
    flex-shrink: 0;
  }
  .close-btn:hover {
    background-color: #eaeef2;
  }
  .title {
    font-weight: 700;
    font-size: 11px;
    color: #1f2328;
    flex: 1;
  }
  .meta-pill {
    padding: 1px 6px;
    border-radius: 999px;
    background-color: #ddf4ff;
    border: 1px solid #54aeff;
    color: #0969da;
    font-weight: 700;
    font-size: 9px;
    white-space: nowrap;
  }
  .controls-bar {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    padding: 3px 5px;
    margin-bottom: 4px;
    background-color: #f6f8fa;
    border: 1px solid #d0d7de;
    border-radius: 3px;
  }
  table {
    border-collapse: collapse;
  }
  th,
  td {
    border-left: 1px solid #c3c3c3;
    border-top: 1px solid #c3c3c3;
    padding: 2px 4px;
    vertical-align: middle;
  }
  th {
    background-color: #f0f3f6;
    color: #1f2328;
  }
  tr.highlight:hover {
    background-color: #40c340;
  }
  .projected {
    background-color: #ddf;
    text-align: right;
  }
  .confirmed {
    background-color: #dd4;
    text-align: right;
  }
  th.sortable {
    cursor: pointer;
    user-select: none;
    white-space: nowrap;
  }
  th.sortable:hover {
    background-color: #e2e8f0;
  }
  .sort-indicator {
    display: inline-block;
    margin-left: 2px;
    font-size: 8px;
    line-height: 1;
    vertical-align: middle;
  }
  .sort-indicator.primary {
    color: #0969da;
  }
  .sort-indicator.secondary {
    color: #6e7781;
  }
  .sort-priority {
    font-size: 7px;
    vertical-align: super;
  }
`;
