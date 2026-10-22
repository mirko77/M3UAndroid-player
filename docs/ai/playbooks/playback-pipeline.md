# Playbook: Playback Pipeline

## When to use this playbook

Use this when the task involves media playback, Media3/ExoPlayer integration, playback controls, casting, video overlays, lifecycle handling, playback persistence, or stream selection.

## Required context

Read these first:

- `AGENTS.md`
- Relevant `app/AGENTS.md`, `business/AGENTS.md`, and `data/AGENTS.md`
- Nearby playback UI, state holder, repository, service/adapter, and tests

## Safe change scope

The agent may modify:

- Playback UI and controls for the affected surface
- Playback state mapping, lifecycle glue, and adapters directly related to the task
- Tests or manual checklist notes for changed behavior

The agent should avoid modifying:

- Parser, persistence, or playlist logic unless playback requires it
- TV focus behavior as a side effect without TV validation
- Broad engine rewrites for localized UI/control bugs

## Architecture rules

- Keep UI state, playback engine state, and persistence concerns separated.
- Handle lifecycle events explicitly and clean up playback resources.
- Keep video overlays readable without obscuring primary playback.
- Treat autoplay, resume, casting, and stream-selection behavior as user-visible compatibility areas.

## Rai live channels

The Rai channels are not plain HLS urls, they are resolved at play time by
`com.m3u.data.playback.rai.RaiChannelResolver`:

- The playlist keeps the relinker url, the resolved url is never stored.
- The resolver asks the relinker for the master playlist, picks one video variant
  (1920x1080 first, then 1280x720) and plays its media playlist, which carries the
  muxed audio and video. The alternate audio renditions and the I-frame playlists of
  the master are not always served by the CDN, so the master is never handed to the
  player.
- Every Rai request uses the single user agent of `RaiChannels.USER_AGENT`, the media
  tokens are bound to the user agent which fetched the master playlist: a different
  user agent answers 403 on the media playlist and on the segments. The resolver and
  the player have to keep using that same value.
- The resolution reads the response bodies, which is a network operation: it runs on the
  io dispatcher and never on the main thread.
- A 403 or a 404 during the playback is not fatal, the channel is resolved again and
  the playback is retried once.
- An already resolved url which was pasted into a playlist is mapped back to its
  channel and resolved through the relinker again.

## Common mistakes

- Starting playback work directly from composition without effect/lifecycle control.
- Persisting transient playback state as durable user data.
- Breaking TV remote control paths while fixing phone controls.
- Changing buffering or retry semantics without PR notes.
- Letting a Rai channel reach the player with another user agent or with the master
  playlist.

## Validation

Run:

```bash
./gradlew :app:smartphone:compileDebugKotlin
```

For TV playback surfaces, also run:

```bash
./gradlew :app:tv:compileDebugKotlin
```

Run playback-related tests or manual lifecycle checks when available.

If the command is not available or cannot be run, explain why.

## PR notes

The PR should mention:

- Playback behavior, lifecycle, or controls changed
- Casting, resume, retry, or overlay risks
- Phone/TV validation coverage
