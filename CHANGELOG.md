v1.3.4
======
* Changed 'wait time' for retries to increase depending on number of attempts made to load page.

v1.3.3
======
* If the Gatherer experiences a HTTP error code `429` (Too many requests), it will pause for 1-20 milliseconds and retry via  recursive method call.

v1.3.2
======
* New reputation columns accidentally deleted from CREATE TABLE script.

v1.3.1
======
* Resolved bug with mismatching number of column headers and values in SQL inserts
* Added option to suppress SSL verification warnings - `-s`

v1.3.0
======
* Completely removed 30 second exclude range from image reader - it was excluding players that are active, and none of the players being excluded are inactive.
* DB field `artbook` renamed to `arrartbook`
* Added parsing for owners of Encyclopedia Eorzea
* Added parsing for owners of Heavensward Art book 1
* Added parsing for owners of Heavensward Art book 2
* Added parsing for owners of Topaz Carbuncle plush
* Added parsing for owners of Emerald Carbuncle plush
* Added parsing for characters who have completed Moogle Beast Tribe to Rank 7
* Added parsing for characters who have completed Vanu Vanu Beast Tribe to Rank 7
* Added parsing for characters who have completed Vath Beast Tribe to Rank 7
* [Bugfix] 180 days and 90 day subscriptions were returing wrong value

v1.2.1
======
* Fixed issue with 'active' players not being defined as active because their profile had been updated in the 5 mins before the run (we built this is in as a feature to stop us defining inactive players as active)

v1.2.0
======
* Added testing of player activity based on last-modified date of full body image on lodestone

v1.1.0
=======
* Added check for 3.3 completion (Wind-up Aymeric)
* Improvements to test suite stability

v1.0.0
=======
* Initial stable release

