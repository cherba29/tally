export default function(value: number) {
  if (value === null || value === undefined) {
    return "n/a";
  }
  return (value / 100.0).toFixed(2).replace(/(\d)(?=(\d\d\d)+(?!\d))/g, "$1,");
}
