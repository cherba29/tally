import { LitElement, css, html, nothing } from 'lit';
import { customElement, property } from 'lit/decorators.js';
import * as d3 from 'd3';
import { currency } from '../format';
import { type GqlMonthTransferSummary } from '../gql_types';
import { Month } from '@tally/lib/core/month';

import './period-buttons';

export interface TransfersSummaryMonthRangeChange {
  accountPath: string;
  startMonth: Month | undefined;
  endMonth: Month;
}


@customElement('transfers-summary-tooltip')
export class TransfersSummaryTooltip extends LitElement {
  static override styles = css`
    td {
      border-left: 1px solid #c3c3c3;
      border-top: 1px solid #c3c3c3;
      padding: 1px;
      vertical-align: middle;
    }
    .index-chart {
      margin: 8px 0;
      background: #fff;
      border: 1px solid #e5e7eb;
    }
  `;

  @property({attribute: false})
  accountPath: string = '';

  __months: string[] = [];
  @property()
  set months(value: string[]) {
    const oldValue = this.__months;
    this.__months = value;
    this.requestUpdate('months', oldValue);
  }
  get months() {
    return this.__months;
  }


  private __monthlyData: GqlMonthTransferSummary[] = [];

  @property()
  set monthlyData(value: GqlMonthTransferSummary[]) {
    const oldValue = this.__monthlyData;
    this.__monthlyData = value;
    this.requestUpdate('monthlyData', oldValue);
  }
  get monthlyData() {
    return this.__monthlyData;
  }
  
  onCloseButton() {
    this.dispatchEvent(new CustomEvent('close'));
  }

  switchView(e: CustomEvent) {
    console.log(`### switchView changed month range ${e.detail.startMonth} - ${e.detail.endMonth}`);
    this.dispatchEvent(
      new CustomEvent<TransfersSummaryMonthRangeChange>('month-range-change', {
        detail: {
          accountPath: this.accountPath,
          startMonth: e.detail.startMonth,
          endMonth: e.detail.endMonth,
        },
      })
    );
  }

  private renderIndexChart(): SVGSVGElement | typeof nothing {
    if (!this.months || !this.monthlyData || this.months.length < 2 || this.monthlyData.length < 2) {
      return nothing;
    }

    const points: Array<{date: Date; value: number}> = [];
    const count = Math.min(this.months.length, this.monthlyData.length);
    for (let i = 0; i < count; i++) {
      const monthStr = this.months[i]!;
      const data = this.monthlyData[i]!;
      try {
        const m = Month.fromString(monthStr);
        const date = new Date(Date.UTC(m.year, m.month, 1));
        const value = Math.max(1, data.totalTransfers);
        points.push({date, value});
      } catch {
        // Skip invalid month
      }
    }

    if (points.length < 2) {
      return nothing;
    }

    // Sort chronologically ascending
    points.sort((a, b) => a.date.getTime() - b.date.getTime());

    const width = 800;
    const height = 240;
    const marginTop = 20;
    const marginRight = 90;
    const marginBottom = 30;
    const marginLeft = 40;

    const x = d3.scaleUtc()
      .domain(d3.extent(points, (d) => d.date) as [Date, Date])
      .range([marginLeft, width - marginRight])
      .clamp(true);

    const v0 = points[0]!.value;
    const series = [
      {
        key: 'Total Transfers',
        values: points.map((d) => ({
          date: d.date,
          value: d.value / v0,
          rawValue: d.value,
        })),
      },
    ];

    const k =
      Math.max(
        1.05,
        d3.max(series, ({values}) => d3.max(values, (d) => d.value)! / d3.min(values, (d) => d.value)!) || 1.05
      );
    const y = d3.scaleLog()
      .domain([1 / k, k])
      .rangeRound([height - marginBottom, marginTop]);

    const z = d3.scaleOrdinal(d3.schemeCategory10)
      .domain(series.map((d) => d.key));

    const bisect = d3.bisector<{date: Date; value: number}, Date>((d) => d.date).center;

    const svg = d3.create('svg')
      .attr('width', width)
      .attr('height', height)
      .attr('viewBox', [0, 0, width, height])
      .attr('style', 'max-width: 100%; height: auto; -webkit-tap-highlight-color: transparent; cursor: crosshair; display: block;');

    // Bottom axis
    svg.append('g')
      .attr('transform', `translate(0,${height - marginBottom})`)
      .call(d3.axisBottom(x).ticks(Math.min(points.length, width / 80)).tickSizeOuter(0))
      .call((g) => g.select('.domain').remove());

    // Left axis
    svg.append('g')
      .attr('transform', `translate(${marginLeft},0)`)
      .call(d3.axisLeft(y).ticks(null, (d: any) => +Number(d).toFixed(6) + '×'))
      .call((g) => g.select('.domain').remove())
      .call((g) =>
        g.selectAll('.tick line')
          .clone()
          .attr('stroke-opacity', (d: any) => (d === 1 ? 0.4 : 0.1))
          .attr('x2', width - marginLeft - marginRight)
      )
      .call((g) => {
        const lastTick = g.select('.tick:last-of-type text');
        if (!lastTick.empty()) {
          lastTick.clone()
            .attr('x', 3)
            .attr('text-anchor', 'start')
            .attr('font-weight', 'bold')
            .text('Growth');
        }
      });

    const rule = svg.append('g')
      .append('line')
      .attr('class', 'rule-line')
      .attr('y1', marginTop)
      .attr('y2', height - marginBottom)
      .attr('stroke', 'black')
      .attr('stroke-opacity', 0.4);

    const serie = svg.append('g')
      .selectAll('g')
      .data(series)
      .join('g');

    const line = d3.line<{date: Date; value: number}>()
      .x((d) => x(d.date))
      .y((d) => y(d.value));

    serie.append('path')
      .attr('fill', 'none')
      .attr('stroke-width', 1.5)
      .attr('stroke-linejoin', 'round')
      .attr('stroke-linecap', 'round')
      .attr('stroke', (d) => z(d.key))
      .attr('d', (d) => line(d.values));

    serie.append('text')
      .attr('font-family', 'sans-serif')
      .attr('font-size', 10)
      .attr('font-weight', 'bold')
      .attr('stroke', 'white')
      .attr('stroke-width', 3)
      .attr('stroke-linejoin', 'round')
      .attr('paint-order', 'stroke fill')
      .attr('x', width - marginRight + 6)
      .attr('y', (d) => y(d.values[d.values.length - 1]!.value))
      .attr('dy', '0.35em')
      .text((d) => d.key);

    function update(date: Date) {
      date = d3.utcDay.round(date);
      rule.attr('transform', `translate(${x(date) + 0.5},0)`);

      serie.attr('transform', ({values}: any) => {
        const i = bisect(values, date);
        const baselineRatio = values[i].value / values[0].value;
        return `translate(0,${y(1) - y(baselineRatio)})`;
      });
    }

    svg.on('mousemove touchmove', function (event: MouseEvent | TouchEvent) {
      const [xm] = d3.pointer(event, this);
      const date = x.invert(xm);
      update(date);
      event.preventDefault();
    });

    update(points[0]!.date);

    return svg.node()!;
  }

  override render() {
    if (!this.months) {
      return nothing;
    }
    return html`
      <span @click="${this.onCloseButton}">XXX</span>
      <period-buttons
        .currentMonth=${Month.fromString(this.months[0]!)}
        @button-click=${this.switchView}
      ></period-buttons>
      <span style="float:right;">${this.accountPath}</span>
      <div class="index-chart">
        ${this.renderIndexChart()}
      </div>
      <table>
        <thead>
          <th style="min-width:20px"></th>
          <th style="min-width:40px">Month</th>
          <th style="min-width:50px">Internal</th>
          <th style="min-width:50px">External</th>
          <th style="min-width:50px">Total</th>
          <th style="min-width:60px">Tot Internal</th>
          <th style="min-width:40px">Internal %</th>
          <th style="min-width:40px">Internal Annl %</th>
          <th style="min-width:40px">Wgt Int Age</th>
          <th style="min-width:60px">Tot External</th>
          <th style="min-width:40px">External %</th>
          <th style="min-width:40px">External Annl %</th>
          <th style="min-width:40px">Wgt Ext Age</th>
          <th style="min-width:60px">Total</th>
          <th style="min-width:40px">Tot Annl %</th>
          <th style="min-width:40px">Wgt Age</th>
          <th style="min-width:60px">Unaccounted</th>
        </thead>
        <tbody>
        ${this.monthlyData.map(
          (data, index) =>
            html`<tr class="highlight">
              <td align="middle">${index + 1}</td>
              <td>${this.months[index]}</td>
              <td align="right">${currency(data.internalTransfers)}</td>
              <td align="right">${currency(data.externalTransfers)}</td>
              <td align="right">${currency(data.totalMonthTransfers)}</td>
              <td align="right">${currency(data.totalInternalTransfers)}</td>
              <td align="right">${data.totalInternalTransfersPrct}</td>
              <td align="right">${data.totalInternalTransfersAnnualPrct}</td>
              <td align="right">${data.weightedInternalAge}</td>
              <td align="right">${currency(data.totalExternalTransfers)}</td>
              <td align="right">${data.totalExternalTransfersPrct}</td>
              <td align="right">${data.totalExternalTransfersAnnualPrct}</td>
              <td align="right">${data.weightedExternalAge}</td>
              <td align="right">${currency(data.totalTransfers)}</td>
              <td align="right">${data.totalAnnualPrct}</td>
              <td align="right">${data.weightedAge}</td>
              <td align="right">${currency(data.unaccounted)}</td>
            </tr>`
        )}
        </tbody>
      </table>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'transfers-summary-tooltip': TransfersSummaryTooltip;
  }
}
