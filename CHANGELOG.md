# Change log

-Command Queue 0.1.0 (2018-10-03)
--------------------------------
- Add `setPaused(boolean)` to temporarily freeze the queue even if receiver is available.

-Command Queue 0.0.3 (2018-09-16)
--------------------------------
- Fix possible edge case of a receiver setting a different receiver in the `receiveCommand` block.

- Add sample code.

-Command Queue 0.0.2 (2018-09-16)
--------------------------------
- Add `hasReceiver()`.

- Fix that emitting an event inside the receiver block could be executed in the wrong order.

-Command Queue 0.0.1 (2018-09-13)
--------------------------------
- Initial Release.