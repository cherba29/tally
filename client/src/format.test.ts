import { expect } from '@esm-bundle/chai'; // Or your preferred ESM-friendly assertion bundle

// import { dateFormat } from "./format";

// describe('date', () => {
//   it('dateFormat', () => {
//     expect(dateFormat(new Date('2021-03-05'))).toEqual('2021-03-05');
//   });
// });



describe('Math suite', () => {
  it('sums numbers correctly', () => {
    const sum: number = 2 + 2;
    expect(sum).to.equal(4);
  });
});
