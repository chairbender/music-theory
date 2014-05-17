;For dealing with intervals
;The interval keyword naming convention is as follows :<quality><number>,
;where quality is a (augmented) d (diminished) m (minor), P (perfect) or M (major).
;there can also be any number of a's or d's to indicate things like doubly
;or triply augmented/diminished intervals. number is any integer
;greater than or equal to 1.
(ns music-theory.interval
  (:use music-theory.note)
  (:use music-theory.tonal-pitch-class))

(defn- line-of-fifths-quality
  "Given an index, returns the quality abbreviation
  (m for minor, d for diminished, P for perfect, M for major, a for augmented,
  dd for doubly diminished, aa for doubly augmented and so on) as a string,
  for the interval name that would be on the line of fifths at that point
  (with 0 being the 'center' (where it is PU), negative values going towards minor and diminished,
  and positive values going towards major and augmented.
  The qualitative descriptors in the line of fifths are arranged with 3 perfects in the center, 4 minors/majors on each side,
  followed by 7 diminished/augmented, and then 7 doubly-diminished/doubly-augmented, and so forth.
  "
  [index]
  (cond
    (and (>= index -1) (<= index 1)) "P"
    (and (>= index -5) (<= index -2)) "m"
    (and (<= index 5) (>= index 2)) "M"
    (<= index -6) (apply str (repeat (inc (quot (- (+ index 6)) 7))  "d"))
    (>= index 6) (apply str (repeat (inc (quot (- index 6) 7))  "a"))
    )
  )



(def ^{:private true} line-of-fifths-number-pattern ["1" "5" "2" "6" "3" "7" "4"])
(defn- line-of-fifths-number
  "Given an index, returns the number of the interval at that point, as a string, (with 'U' being unison)
  0 is defined to be U, and the pattern as one goes lower than U is 4, 7, 3, 6, 2, 5, U (repeats).
  As the index increases, the pattern is 5, 2, 6, 3, 7, 4, U (repeats)."
  [index]
  (if (>= index 0)
    (get line-of-fifths-number-pattern (mod index (count line-of-fifths-number-pattern)))
    (get (vec (reverse line-of-fifths-number-pattern)) (mod (- (inc index)) (count line-of-fifths-number-pattern))))
  )

(defn- line-of-fifths-interval
  "Given an index into the line of fifths, return an interval name.
  The 0th index is the 'center' of the line of fifths (the point where the interval
  is PU). integers below that walk 'down' the line of fifths (towards minor then diminished intevals),
  and integers above that walk 'up'."
  [index]
  (keyword (str (line-of-fifths-quality index) (line-of-fifths-number index)))
  )

(def ^{:private true} line-of-fifths-letter-pattern ["F" "C" "G" "D" "A" "E" "B"])

(defn- letter-index-offset
  "letter index is for the line of fifths wtih 0 as F and repeating
  as index increases (C  D A E B F C ... and for negative numbers as well). This gives the
  new letter index given a starting tpc and an offset integer"
  [tonal-pitch-class offset]
  (+ offset (.indexOf line-of-fifths-letter-pattern (name (natural tonal-pitch-class))))
  )

(defn- line-of-fifths-letter
  "Return the letter of the tonal pitch class that exists at the given index
  in the line of fifths centered at tonal-pitch-class, as a string"
  [tonal-pitch-class index]
  (get line-of-fifths-letter-pattern
       (mod (letter-index-offset tonal-pitch-class index)
            (count line-of-fifths-letter-pattern)))
  )

(defn- alteration-offset
  "The 'alteration offset' means the difference in 'alteration classes'
  between two tonal pitch classes. the indexes go bb, b, none, #, ##,
  So for example the alteration offset from A## of Bb is -3."
  [starting-tonal-pitch-class offset-tonal-pitch-class]
  (+ (- (sharps offset-tonal-pitch-class) (sharps starting-tonal-pitch-class))
     (- (flats starting-tonal-pitch-class) (flats offset-tonal-pitch-class))))

(defn- letter-offset
  "The difference in index between the letter names of the given TPCs
  (with the index coming from the circle of fifths)."
  [starting-tonal-pitch-class offset-tonal-pitch-class]
  (- (.indexOf line-of-fifths-letter-pattern (name (natural offset-tonal-pitch-class)))
     (.indexOf line-of-fifths-letter-pattern (name (natural starting-tonal-pitch-class)))))

(defn- line-of-fifths-tonal-pitch-class-index
  "Returns the index of tonal-pitch-class on the line of fifths
  centered at center (center being a tonal pitch class as well).
  The index is defined to be 0 at the center and increasingly positive as
  sharps are added, increasingly negative as flats are added"
  [center tonal-pitch-class]
  (+ (* 7 (alteration-offset center tonal-pitch-class))
     (letter-offset center tonal-pitch-class))
  )

(defn- line-of-fifths-alterations
  "Return the alterations of the tonal pitch class that exists at the given index
  in the line of fifths centered at tonal-pitch-class, as a string."
  [tonal-pitch-class index]
  ;alteration index - 0 = none, -1 = b, -2 = bb, 1 = #, 2 = ##
  (let [alteration-index
        (+ (quot (if (>= (letter-index-offset tonal-pitch-class index) 0)
                   (letter-index-offset tonal-pitch-class index)
                   (- (letter-index-offset tonal-pitch-class index) (count line-of-fifths-letter-pattern)))
                 (count line-of-fifths-letter-pattern))
           (if (> (sharps tonal-pitch-class) 0)
             (sharps tonal-pitch-class)
             (- (flats tonal-pitch-class))))]
    (if (> alteration-index 0)
      (apply str (repeat alteration-index "#"))
      (apply str (repeat (- alteration-index) "b")))
    )
  )

(defn- line-of-fifths-tonal-pitch-class
  "Given a center tonal pitch class and an index, return the tonal pitch class
  that exists at that index in the line of fifths centered at that tpc, as a TPC keyword"
  [tonal-pitch-class index]
  (keyword (str (line-of-fifths-letter tonal-pitch-class index) (line-of-fifths-alterations tonal-pitch-class index)))
  )

(defn interval-octaves
  "Return a count of the number of octaves other-note is
  above note (so negative if other-note is below). it is considered an octive or more
  based on the actual note name, not the enharmonic pitch. For example, even though
  A3 G##3 is equivalent to A3 A4, it is not counted as an octave. A3 A4 would return 1."
  [note other-note]
  (quot
    (+
      (* 8 (- (note-octave other-note) (note-octave note)))
      (- (int (note-letter other-note)) (int (note-letter note))))
    8))

(defn interval
  "constructs an interval keyword given a quality string
   (like a or P) and interval number integer."
  [quality number]
  (keyword (str quality number)))

(defn interval-quality
  "Returns a string of the interval quality abbreviation of the given interval keyword."
  [interval]
  (re-find #"[daPMm]+" (name interval))
  )

(defn interval-number
  "returns an integer indicating the interval's number. interval
  should be an interval keyword."
  [interval]
  (read-string (re-find #"\d+" (name interval))))

(defn lower-note
  "returns the note that would appear lower on the staff (lower letter and octave),
  even if one note is enharmonically higher than the other in pitch (so E#4 would be below Fb4
  even though it has a higher pitch)"
  [note other-note]
  (if (or (> (note-octave note) (note-octave other-note))
          (and (= (note-octave note) (note-octave other-note))
               (> (int (note-letter note)) (int (note-letter other-note)))))
    other-note
    note
    ))

(defn higher-note
  "opposite of lower-note"
  [note other-note]
  (if (= note (lower-note note other-note))
    other-note
    note))

(defn interval-keyword
  "Returns the interval name for the interval betwwen the notes.
  Always defines the interval with respect to the lower note, so order
  of the parameters doesn't matter
  For intervals larger than an octave, uses an
  interval name like :M10 or :P12"
  [note other-note]
  (let [lower-note (lower-note note other-note)
        higher-note (higher-note note other-note)]
      (let [octaves (interval-octaves lower-note higher-note)
        base-interval (line-of-fifths-interval
                        (line-of-fifths-tonal-pitch-class-index
                          (note-tonal-pitch-class lower-note)
                          (note-tonal-pitch-class higher-note)))]
    (interval (interval-quality base-interval) (+ (* 7 octaves) (interval-number base-interval))))))

(defn compound-interval-keyword
  "Like interval-keyword, but uses the compound interval name for intervals larger
  than a perfect octave (:P8). So an interval of a major tenth would return :M3"
  [note other-note]
  (line-of-fifths-interval
    (line-of-fifths-tonal-pitch-class-index
      (note-tonal-pitch-class note)
      (note-tonal-pitch-class other-note))))