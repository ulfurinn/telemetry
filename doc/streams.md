# `telemetry.streams`

## `(part-time-carrying dt init next empty add finish)`, `(part-time-carrying dt init next empty add side-effects finish)`

Similar to `riemann.streams/part-time-simple`. Useful for implementing sliding windows.

`(init)` should return the initial state. Called once when the stream is instantiated.

`(next state start end)` should return the state for the next window, carrying over old events as necessary.

`(empty state)` should return true if the state is considered empty. If the state is not empty, a new window will be immediately started, i.e. the stream will continue emitting events until the state is filtered down to nothing.

`(finish state start end)` is only called is the state is not considered empty.

## `(sliding-window flush-interval filter-fn & children)`

Accumulates incoming events in a list. Every `flush-interval` seconds, filters the list through `(filter-fn list start end)`. The result, if not empty, is emitted to children and used to initialize the next window.

## `(sliding-time-window window-interval flush-interval & children)`

Every `flush-interval` seconds, emits a list of events received in the last `window-interval` seconds, where `window-interval >= flush-interval`.

For example, suppose you want to emit data to Graphite every 60s, but incoming events are not frequent enough to collect useful information within that time, e.g. you want to measure the 99th percentile but can't get 100 samples within 60s.
