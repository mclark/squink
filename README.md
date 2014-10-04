# squink

A Clojure library that shortens urls. Includes a simple dockerfile to generate a container.

Based on the Murmur3 Hash, this shortener is designed to always give the same hash for a given url.
By default it uses a 128 bit hash, but also supports 32 bit.

Regardless of the bit length, the hash code is converted to base 62 (0-9, a-z, A-Z) and then a progressive
stem check is used to find the shortest possible unique hash for the given url.  Over time this of
course will get longer, but can be configured to start as low as 1 character. As the existence check
is memoized, setting it this low may be foolish, but shouldn't be completely infeasible.

## Usage

currently `lein uberjar` to compile, then `java -jar <path to uberjar>`
docker run blah blah

## TODO
tests!
swappable hash algorithm
documentation
reorganization of code (private methods, etc)
analytics data

## License

Copyright © 2014 Matt Clark

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
