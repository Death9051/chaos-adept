#!/usr/bin/env bash
# Клиентский тест CI: клиент на виртуальном экране + скриншоты по ходу прогона.
#
# Скриншоты пишутся в story/run/clienttest/ci-screenshots/ и уходят в ветку ci-logs:
#   NN-title.jpg      — главное меню (ресурсы и модели загрузились)
#   NN-world-*.jpg    — мир: сразу после входа и дальше раз в SHOT_EVERY секунд
#   NN-tick-*.jpg     — фоновые снимки раз в TICK_EVERY секунд (загрузка, ошибки, зависания)
# Тест пройден, если в логе есть маркер ADEPT_CLIENTTEST_OK.
set -uo pipefail

RUN_DIR=story/run/clienttest
SHOTS="$RUN_DIR/ci-screenshots"
LOG="$RUN_DIR/logs/latest.log"
DISPLAY_NUM=:99
SHOT_EVERY=3
TICK_EVERY=15
MAX_SHOTS=24
TIMEOUT=20m

mkdir -p "$SHOTS" "$RUN_DIR/logs"
rm -f "$LOG"

Xvfb "$DISPLAY_NUM" -screen 0 1280x720x24 -nolisten tcp &
XVFB_PID=$!
export DISPLAY=$DISPLAY_NUM
sleep 2

shot_count=0
shot() {
  # $1 — имя снимка без номера. Снимаем окно игры; если окна нет, весь экран.
  [ "$shot_count" -ge "$MAX_SHOTS" ] && return
  shot_count=$((shot_count + 1))
  local file
  file=$(printf '%s/%02d-%s.jpg' "$SHOTS" "$shot_count" "$1")
  local win
  win=$(xdotool search --name 'Minecraft' 2>/dev/null | head -n1 || true)
  if [ -n "$win" ]; then
    import -window "$win" -quality 85 "$file" 2>/dev/null || import -window root -quality 85 "$file" 2>/dev/null
  else
    import -window root -quality 85 "$file" 2>/dev/null
  fi
  echo "скриншот: $file"
}

timeout "$TIMEOUT" ./gradlew :story:runClientTest --stacktrace &
GAME_PID=$!

state=wait_title
last_world=0
last_tick=$(date +%s)
while kill -0 "$GAME_PID" 2>/dev/null; do
  now=$(date +%s)
  case "$state" in
    wait_title)
      if grep -q "главное меню открыто" "$LOG" 2>/dev/null; then
        sleep 1; shot title; state=wait_world
      fi ;;
    wait_world)
      if grep -q "игрок в мире" "$LOG" 2>/dev/null; then
        sleep 2; shot world-enter; last_world=$(date +%s); state=in_world
      fi ;;
    in_world)
      if grep -q "ADEPT_CLIENTTEST_OK" "$LOG" 2>/dev/null; then
        state=done
      elif [ $((now - last_world)) -ge "$SHOT_EVERY" ]; then
        shot world; last_world=$now
      fi ;;
  esac
  if [ "$state" != in_world ] && [ "$state" != done ] && [ $((now - last_tick)) -ge "$TICK_EVERY" ]; then
    shot tick; last_tick=$now
  fi
  sleep 1
done
wait "$GAME_PID"
echo "клиент завершился с кодом $?"
kill "$XVFB_PID" 2>/dev/null || true

if grep -q "ADEPT_CLIENTTEST_OK" "$LOG" 2>/dev/null; then
  echo "Клиентский тест пройден, скриншотов: $shot_count"
else
  echo "::error::Маркер ADEPT_CLIENTTEST_OK не найден в $LOG"
  tail -n 80 "$LOG" 2>/dev/null || true
  exit 1
fi
