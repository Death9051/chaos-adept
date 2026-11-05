#!/usr/bin/env bash
# Клиентский тест CI: клиент на виртуальном экране + скриншоты по ходу прогона.
# Общий для clienttest и fullpack: задача Gradle и папка запуска задаются переменными.
#
#   GRADLE_TASK  задача запуска клиента       (по умолчанию :story:runClientTest)
#   RUN_DIR      папка игры этого запуска     (по умолчанию story/run/clienttest)
#
# Скриншоты пишутся в $RUN_DIR/ci-screenshots/ и уходят в ветку ci-logs:
#   NN-scene-<имя>.jpg — сцена ядра: снимок через секунду после строки «ADEPT_SCENE <имя>» в логе
#   NN-world-enter.jpg — сразу после входа в мир
#   NN-world.jpg       — мир после сцен, раз в SHOT_EVERY секунд
#   NN-tick.jpg        — до входа в мир раз в TICK_EVERY секунд (загрузка, ошибки, зависания)
#   NN-stuck.jpg       — меню не открылось за TITLE_DEADLINE секунд
# Тест пройден, если в логе есть маркер ADEPT_CLIENTTEST_OK.
set -uo pipefail

GRADLE_TASK=${GRADLE_TASK:-:story:runClientTest}
RUN_DIR=${RUN_DIR:-story/run/clienttest}
SHOTS="$RUN_DIR/ci-screenshots"
LOG="$RUN_DIR/logs/latest.log"
DISPLAY_NUM=${DISPLAY_NUM:-:99}
SHOT_EVERY=${SHOT_EVERY:-5}
TICK_EVERY=15
MAX_SHOTS=${MAX_SHOTS:-30}
TIMEOUT=${TIMEOUT:-20m}
# Если меню не открылось за это время — игра застряла (экран ошибки модов и т. п.).
TITLE_DEADLINE=${TITLE_DEADLINE:-360}

mkdir -p "$SHOTS" "$RUN_DIR/logs"
rm -f "$LOG"

Xvfb "$DISPLAY_NUM" -screen 0 1280x720x24 -nolisten tcp &
XVFB_PID=$!
export DISPLAY=$DISPLAY_NUM
sleep 2

shot_count=0
shot() {
  # $1 — имя снимка без номера. Снимаем только окно игры.
  [ "$shot_count" -ge "$MAX_SHOTS" ] && return
  local win
  win=$(xdotool search --name 'Minecraft' 2>/dev/null | head -n1 || true)
  # Окна игры ещё нет — снимать нечего (был бы чёрный кадр).
  [ -z "$win" ] && return
  shot_count=$((shot_count + 1))
  local file
  file=$(printf '%s/%02d-%s.jpg' "$SHOTS" "$shot_count" "$1")
  import -window "$win" -quality 85 "$file" 2>/dev/null || import -window root -quality 85 "$file" 2>/dev/null
  echo "скриншот: $file"
}

timeout "$TIMEOUT" ./gradlew "$GRADLE_TASK" --stacktrace &
GAME_PID=$!

state=wait_title
started=$(date +%s)
last_world=0
last_scene=0
last_tick=$started
scenes_done=0
while kill -0 "$GAME_PID" 2>/dev/null; do
  now=$(date +%s)
  case "$state" in
    wait_title)
      # Меню сразу сменяется созданием мира, поэтому отдельного снимка меню нет.
      if grep -q "главное меню открыто" "$LOG" 2>/dev/null; then
        state=wait_world
      elif [ $((now - started)) -ge "$TITLE_DEADLINE" ]; then
        echo "::error::Главное меню не открылось за $TITLE_DEADLINE с — см. скриншоты tick и stuck"
        shot stuck
        pkill -f 'net.neoforged' 2>/dev/null || true
        kill "$GAME_PID" 2>/dev/null || true
        break
      fi ;;
    wait_world)
      if grep -q "игрок в мире" "$LOG" 2>/dev/null; then
        sleep 2; shot world-enter; last_scene=$(date +%s); last_world=$last_scene; state=in_world
      fi ;;
    in_world)
      # Сцены ядра: каждая новая строка ADEPT_SCENE — снимок через секунду (кадр держится 3 с).
      mapfile -t scenes < <(grep -o 'ADEPT_SCENE [A-Za-z0-9_]*' "$LOG" 2>/dev/null | awk '{print $2}')
      while [ "$scenes_done" -lt "${#scenes[@]}" ]; do
        sleep 1
        shot "scene-${scenes[$scenes_done]}"
        scenes_done=$((scenes_done + 1))
        last_scene=$(date +%s)
      done
      if grep -q "ADEPT_CLIENTTEST_OK" "$LOG" 2>/dev/null; then
        state=done
      elif [ $((now - last_scene)) -ge 8 ] && [ $((now - last_world)) -ge "$SHOT_EVERY" ]; then
        # Сцен больше нет (между сценами ~4,5 с) — обычные снимки мира.
        shot world; last_world=$now
      fi ;;
  esac
  if [ "$state" = wait_title ] || [ "$state" = wait_world ]; then
    if [ $((now - last_tick)) -ge "$TICK_EVERY" ]; then
      shot tick; last_tick=$now
    fi
  fi
  sleep 1
done
wait "$GAME_PID"
echo "клиент завершился с кодом $?"
kill "$XVFB_PID" 2>/dev/null || true

if grep -q "ADEPT_CLIENTTEST_OK" "$LOG" 2>/dev/null; then
  echo "Клиентский тест пройден, сцен: $scenes_done, скриншотов: $shot_count"
else
  echo "::error::Маркер ADEPT_CLIENTTEST_OK не найден в $LOG"
  tail -n 80 "$LOG" 2>/dev/null || true
  exit 1
fi
