(use-modules (sxml simple)
             (ice-9 pretty-print)
             (ice-9 format)
             (ice-9 match)
             (ice-9 local-eval)
             (ice-9 receive)
             (ice-9 regex)
             (ice-9 threads)
             (srfi srfi-1))
(use-modules (statprof))


(define debug-enabled      #t)
(define unit-tests-enabled #f)

(define-syntax-rule (defsyntax (name syntax-var) body)
  (define-syntax name (λ (syntax-var) body)))
(define-syntax-rule (try (body ...) key-sym (args ...) (handler ...))
  (catch key-sym (λ () body ...) (λ* (args ...) handler ...)))
(define-syntax-rule (try-all (body ...) (key args) (handler ...))
  (try (body ...) #t (key #:rest args) (handler ... (throw key args))))
(define-syntax-rule (λ* args ...)
  (lambda* args ...))
(define-syntax-rule (thunk args ...)
  (lambda* () args ...))

(define* (head        xs)         (cons (car xs) '()))
(define* (tail        xs)         (cdr xs))
(define* (not-null?   xs)         (not (null? xs)))
(define* (many?       xs)         (> 1 (length xs)))
(define* (one?        xs)         (= 1 (length xs)))
(define* (filter-null xs)         (filter not-null? xs))
(define* (streq?      a b)        (string=? a b))
(define* (const-null  #:rest xs) '())

(define* (printf fmt #:rest args) (apply format `(#t ,fmt ,@args)))
(define* (fmtstr fmt #:rest args) (apply format `(#f ,fmt ,@args)))

(define* (printfln fmt #:rest args)
  (apply printf `(,(string-append ";; " fmt "\n") ,@args)))

(define* (errorfmt fmt #:rest args)
  (error (apply fmtstr `(,(fmtstr ";; Error: ~s" fmt) ,@args))))

(define* (profile prof-thunk
                  #:key
                  (sample-ms    10)
                  (display?     #f)
                  (count-calls? #f)
                  (full-stacks? #f))
  (statprof-stop)
  (receive (sr-s sr-us)
      (floor/ (* 1000 sample-ms) 1000000)
    (statprof-reset sr-s sr-us
                    count-calls?
                    full-stacks?))
  (statprof-start)
  (letrec
      ((statprof-string (thunk (with-output-to-string statprof-display)))
       (sed             regexp-substitute/global)
       (comment-string  (λ* (str) (sed #f "\n" str 'pre "\n;; " 'post)))
       (res             (prof-thunk)))
    (statprof-stop)
    (when display? (display (comment-string (statprof-string))))
    res))

(define* (list-intersperse src-l elem)
  (if (null? src-l) src-l
      (let loop ((l (cdr src-l)) (dest (cons (car src-l) '())))
        (if (null? l) (reverse dest)
            (loop (cdr l) (cons (car l) (cons elem dest)))))))

(define* (cleanup-syntax stx)
  (try-all
   ((letrec
        ([add-c-err  (λ* (str) (errorfmt "add-c: ~s" str))]
         [add-c      (λ* (#:rest args)
                         (when (null? args) (add-c-err "not enough args"))
                         `(cleanup ',(car args)
                                   ,@(map (λ* (x) (if (list? x)
                                                      (add-c-list x)
                                                      x))
                                          (cdr args))))]
         [add-c-list (λ* (xs) (apply add-c xs))]

         [cleanup    (λ* (func #:rest args)
                         (letrec
                             ([fargs   (filter-null args)]
                              [fst     (thunk (car fargs))]
                              [err     (λ* (fn) (errorfmt "~s: (~s . ~s)"
                                                          fn func fargs))]
                              [pnum    string->number]
                              [psym    string->symbol]
                              [to-str  (thunk (apply string-append fargs))]
                              [to-num  (thunk
                                        (cond
                                         ((string? (fst)) (pnum (fst)))
                                         ((number? (fst)) (fst))
                                         (#t              (err 'to-num))))]
                              [to-sym  (thunk
                                        (cond
                                         ((string? (fst)) `',(psym (fst)))
                                         ((symbol? (fst)) (fst))
                                         (#t              (err 'to-sym))))]
                              [to-func (λ* (fn) (thunk `(,fn ,@fargs)))]
                              [to-list (to-func 'many)]
                              [ident   (to-func func)]
                              [to-null (thunk '())])
                           (match func
                             ['space                 " "]
                             ['newline               (to-null)]
                             ['comment               (to-null)]
                             ['introduce             (to-null)]
                             ['keyword               (to-str)]
                             ['value                 (to-str)]
                             ['string                (to-str)]
                             ['integer               (to-num)]
                             ['float                 (to-num)]
                             ['name                  (to-str)]
                             ['ref                   (to-str)]
                             ['match-equations       (to-list)]
                             ['let-definitions       (to-list)]
                             ['let-scope             (to-list)]
                             ['letrec-definitions    (to-list)]
                             ['letrec-scope          (to-list)]
                             ['lam-var               (to-str)]
                             ['lam-vars              (to-list)]
                             ['lam-body              (to-list)]
                             ['function              (to-str)]
                             ['type-definition-vars  (to-list)]
                             ['type-definition-cons  (to-list)]
                             ['constructor-arguments (to-list)]
                             [_                      (ident)])))])
      (local-eval (if (list? stx) (add-c-list stx) stx) (the-environment))))

   (k a)
   ((printfln ";; Error in cleanup-syntax: key = ~s, args = ~s" k a))))

(define* (run-cleanup stx)
  (cons (car stx) (map cleanup-syntax (cdr stx))))


(define* (render-syntax stx)
  (try-all
   ((letrec
        ((joining     (λ* (list between)
                          (apply string-append
                                 (list-intersperse list between))))
         (render      (λ* (x) (render-syntax x)))
         (type-vars   (λ* (vs) (joining (map render vs) ", ")))
         (type-cons   (λ* (cs) (joining (map render cs) " | ")))
         (con-args    (λ* (as) (joining (map render as) " * ")))
         (app-args    (λ* (as) (joining (map render as) " ")))

         (type        (λ* (n vs cs)
                          (fmtstr "type ~a ~a = ~a;;\n"
                                  (if (null? vs)
                                      ""
                                      (string-append
                                       "(" (type-vars vs) ")"))
                                  n (type-cons cs))))
         (con         (λ* (cn as)
                          (if (null? as)
                              cn
                              (fmtstr "~s of (~s)" cn (con-args as)))))
         (conditional (λ* (c t f)
                          (fmtstr "(if ~s then ~s else ~s)" c t f)))
         (match       (λ* (v es) (fmtstr "(match ~a with ~a)"
                                         v (joining (map render es)
                                                    " | "))))
         (match-eqn   (λ* (p v)  (fmtstr "~a -> ~a" p v)))
         (let-in      (λ* (es s) (fmtstr "(let ~a in ~a)"
                                         (joining (map render es) "; ") s)))
         (def         (λ* (es #:optional (s '()))
                          (if (null? s)
                              (fmtstr "let ~a;;"
                                      (joining (map render es) "; "))
                              (fmtstr "let ~a in ~a;;"
                                      (joining (map render es) "; ") s))))
         (let-eqn     (λ* (n v) (fmtstr "~a = (~a)" n v)))
         (letr-in   (λ* (es s) (fmtstr "(let rec ~a in ~a)"
                                       (joining (map render es) "; ") s)))
         (defr        (λ* (es #:optional (s '()))
                          (if (null? s)
                              (fmtstr "let rec ~a;;"
                                      (joining (map render es) "; "))
                              (fmtstr "let rec ~a in ~a;;"
                                      (joining (map render es) "; ") s))))
         (letr-eqn    (λ* (n v) (fmtstr "~a = (~a)" n v)))
         (lam         (λ* (vs bd) (fmtstr "(fun ~a -> ~a)"
                                          (joining (map symbol->string vs) " ")
                                          (map render bd))))
         (app         (λ* (f #:rest as)
                          (if (symbol? f)
                              (fmtstr "(~a ~a)"
                                      (symbol->string f)
                                      (app-args as))
                              (fmtstr "(~a ~a)"
                                      (render f)
                                      (app-args as))))))
      (local-eval stx (the-environment))))
   (k a)
   ((printfln "Error in render-syntax: key = ~s, args = ~s" k a))))















(define* (normalize func #:rest args)
  (catch 'return
    (thunk
     (letrec
         ([return    (λ* (retval) (throw 'return retval))]
          [arglen    (thunk (length args))]
          [err       (λ* [fn] (errorfmt "~s: (~s . ~s)" fn func args))]
          [render    (λ* (x) (render-syntax (normalize-syntax x)))]
          [to-rend   (thunk (apply string-append (par-map render args)))]
          [to-fst    (thunk
                      (if (one? args)
                          (normalize-syntax (car args))
                          (err 'to-fst)))]
          [to-func   (λ* [#:optional fn]
                         (if (not fn)
                             (to-func func)
                             (cons fn (map normalize-syntax args))))]
          [to-func-n (λ* [n #:rest fn]
                         (cond
                          [(and (number? n) (= n (arglen)))
                           (apply to-func-n `((,n) ,@fn))]
                          [(and (list? n) (member (arglen) n))
                           (apply to-func fn)]
                          [#t (err `(to-func-n ,n ,fn))]))])
       (when (string? func)
         (apply string-append `(,func ,@(map normalize-syntax args))))
       (unless (symbol? func) (err 'normalize))
       (match func
         ['body                  (to-func                     )]
         ['many                  (to-func                     )]
         ['rend                  (to-rend                     )]
         ['paren                 (to-fst                      )]
         ['name                  (to-fst                      )]
         ['integer               (to-fst                      )]
         ['float                 (to-fst                      )]
         ['string                (to-fst                      )]
         ['conditional           (to-func-n   3   'conditional)]
         ['match-expression      (to-func-n   2         'match)]
         ['match-input           (to-fst                      )]
         ['match-equation        (to-func-n   2     'match-eqn)]
         ['match-equation-val    (to-fst                      )]
         ['match-equation-pat    (to-fst                      )]
         ['let-expression        (to-func-n   2        'let-in)]
         ['let-declaration       (to-func-n '(1 2)        'def)]
         ['let-equation          (to-func-n   2       'let-eqn)]
         ['let-equation-name     (to-fst                      )]
         ['let-equation-val      (to-fst                      )]
         ['letrec-expression     (to-func-n           'letr-in)]
         ['letrec-declaration    (to-func-n '(1 2)       'defr)]
         ['letrec-equation       (to-func-n   2      'letr-eqn)]
         ['letrec-equation-name  (to-fst                      )]
         ['letrec-equation-val   (to-fst                      )]
         ['lam                   (to-func-n   2               )]
         ['application           (to-func                 'app)]
         ['argument              (to-fst                      )]
         ['type-definition       (to-func-n   3          'type)]
         ['type-definition-name  (to-fst                      )]
         ['type-definition-var   (to-fst                      )]
         ['constructor           (to-func-n '(1 2)        'con)]
         ['constructor-name      (to-fst                      )]
         ['constructor-argument  (to-fst                      )]
         [_                      (to-func                     )])))
    (λ (retval) retval)))

(define* (normalize-syntax stx) (if (list? stx) (apply normalize stx) stx))

(define* (run-normalize stx)
  (try-all
   ((cons (car stx) (map normalize-syntax (cdr stx))))
   (k a) ((printfln ";; Error in run-normalize: key = ~s, args = ~s" k a))))



(define* (renormalize func #:rest args)
  (letrec
      ([norml     (λ* (list) (map renormalize-syntax list))]
       [norm      (thunk (norml args))]
       [to-list   (thunk (cdr (to-func)))]
       [to-eqn    (thunk (if (= (length args) 2)
                             `[,(car args) => ,@(norml (cdr args))]
                             (throw 'renormalize-error func args)))]
       [to-func   (λ* [#:optional fn]
                      (cons (if (not fn) func fn) (norm)))])
    (if (not (symbol? func)) (norml (cons func args))
        (match func
          ['many        (to-list        )]
          ['conditional (to-func     'if)]
          ['match-eqn   (to-eqn         )]
          ['let-eqn     (to-eqn         )]
          ['letr-eqn    (to-eqn         )]
          ['let-in      (to-func   'olet)]
          ['letr-in     (to-func  'oletr)]
          ['def         (to-func   'olet)]
          ['defr        (to-func  'oletr)]
          [_            (to-func        )]))))

(define* (renormalize-syntax stx)
  (if (list? stx) (apply renormalize stx) stx))

(define* (run-renormalize stx)
  (try-all
   ((renormalize-syntax stx))
   (k a) ((printfln ";; Error in run-renormalize: key = ~s, args = ~s" k a))))

(define* (read-xml path)
  (let* ((port (open-file path "r"))
         (xml (cdr (xml->sxml port))))
    (close-port port)
    xml))

(define* (process-data input-data)
  (let* ([cleaned (run-cleanup     input-data)]
         [normal  (run-normalize      cleaned)]
         [final   (run-renormalize     normal)])
    final))

(define* (main)
  (try-all
   ((catch 'exit
      (thunk
       (unless (= (length (command-line)) 2)
         (printfln "Usage: normalize.scm <path-to-xml>")
         (throw 'exit 1))

       (let ((input-path (list-ref (command-line) 1)))
         (unless (access? input-path R_OK)
           (printfln "File not found. Quitting.")
           (throw 'exit 2))
         (pretty-print
          (process-data (car (read-xml input-path))))
         (throw 'exit 0)))

      (λ* (key code)
          (printfln "Exit code: ~s" code)
          (exit code))))
   (k a)
   ((printfln ";; Error in main: key = ~s, args = ~s" k a))))


;; Unit test input data and the relevant expected output from `process-data'


(define unit-test-1-input
  '(body
    (application (function "hello")
                 (argument "a")
                 (comment "comment")
                 (introduce "x")
                 (argument "b")
                 (argument "c"))
    (application (function "goodbye")
                 (argument "x")
                 (argument "y"))))

(define unit-test-1-expected
  '(body (app "hello" "a" "b" "c") (app "goodbye" "x" "y")))

(define* (unit-test-1)
  (equal? (process-data unit-test-1-input) unit-test-1-expected))

(define unit-test-2-input
  '(body
    (type-definition
     (type-definition-name (name "sort"))
     (type-definition-vars)
     (type-definition-cons
      (constructor
       (constructor-name "SortId"))
      (constructor
       (constructor-name "SortStmt"))))))

(define unit-test-2-expected
  '(body (type "sort" () ((con "SortId") (con "SortStmt")))))

(define* (unit-test-2)
  (equal? (process-data unit-test-2-input) unit-test-2-expected))

(define unit-test-3-input
  '(body
    (let-declaration
     (let-definitions
      (let-equation
       (let-equation-name "testValue")
       (let-equation-val
        (application
         (lam
          (lam-vars
           (lam-var "x")
           (lam-var "y"))
          (lam-body
           (application
            (function "add")
            (argument "x")
            (argument "y"))))
         (argument (integer "5"))
         (argument (integer "7")))))))))

(define unit-test-3-expected
  '(body (olet (("testValue" => (app (lam ("x" "y")
                                          ((app "add" "x" "y")))
                                     5 7))))))

(define* (unit-test-3)
  (equal? (process-data unit-test-3-input) unit-test-3-expected))

(define unit-test-4-input
  '(body
    (let-declaration
     (let-definitions
      (let-equation
       (let-equation-name "testValue")
       (let-equation-val
        (match-expression
         (match-input "True")
         (match-equations
          (match-equation
           (match-equation-pat
            (rend "True"
                  (space)
                  "when"
                  (space)
                  (rend
                   (application
                    (function "test")
                    (argument "x")))))
           (match-equation-val (integer "0")))
          (match-equation
           (match-equation-pat "True")
           (match-equation-val (integer "1")))))))))))

(define unit-test-4-expected
  '(body (olet (("testValue" =>
                 (match "True"
                   (("True when (test x)" => 0)
                    ("True" => 1))))))))

(define* (unit-test-4)
  (equal? (process-data unit-test-4-input) unit-test-4-expected))

(define unit-test-5-input
  '(body
    (letrec-declaration
     (letrec-definitions
      (letrec-equation
       (letrec-equation-name "testValue")
       (letrec-equation-val "5"))))))

(define unit-test-5-expected
  '(body (oletr (("testValue" => "5")))))

(define* (unit-test-5)
  (equal? (process-data unit-test-5-input) unit-test-5-expected))


(define* (unit-tests)
  (try-all
   ((unless (unit-test-1) (throw 'unit-test 1))
    (unless (unit-test-2) (throw 'unit-test 2))
    (unless (unit-test-3) (throw 'unit-test 3))
    (unless (unit-test-4) (throw 'unit-test 4))
    (unless (unit-test-5) (throw 'unit-test 5))
    #t)
   (k a) ((match k
            ['unit-test (printfln "Error in unit-test-~d" (car a))]
            [_          (apply throw (cons k a))                  ]))))



(when unit-tests-enabled (unit-tests))

(unless debug-enabled (main))
