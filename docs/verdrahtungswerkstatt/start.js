// Erst alle Bereiche verbinden, dann die Oberfläche und die Bridge starten.
import {createContext} from './zustand.js?v=20261013b';
import {createEditor} from './editor.js?v=20261013b';
import {createProject} from './projekt.js?v=20261013b';
import {createSimulation} from './simulation.js?v=20261013b';

let started;
export function bootstrap() {
  if (started) return started;
  const ctx = createContext();
  ctx.services.editor = createEditor(ctx);
  ctx.services.projekt = createProject(ctx);
  ctx.services.simulation = createSimulation(ctx);
  started = ctx;
  ctx.services.simulation.bindEvents();
  ctx.services.editor.bindEvents();
  ctx.services.projekt.bindEvents();
  ctx.services.projekt.loadInitial();
  ctx.services.simulation.connectEngine();
  const help = document.createElement('script');
  help.src = 'egt-student-help.js?v=20261012d';
  document.body.append(help);
  return ctx;
}
bootstrap();
