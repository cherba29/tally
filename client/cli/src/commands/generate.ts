import type { Arguments, CommandBuilder } from 'yargs';

type Options = {
  name: string;
  upper: boolean | undefined;
};

export const command: string = 'generate <name>';
export const desc: string = 'Greet <name> with Hello';

export const builder: CommandBuilder<Options, Options> = (yargs) =>
  yargs
    .options({
      upper: { type: 'boolean' },
    })
    .positional('name', { type: 'string', demandOption: true });

export const handler = (argv: Arguments<Options>): void => {
  const { name, upper } = argv;
  const greeting = `Hello, ${name}!\n`;
  process.stdout.write(upper ? greeting.toUpperCase() : greeting);
  process.exit(0);
};
