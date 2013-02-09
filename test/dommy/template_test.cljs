(ns dommy.template-test
  (:require [dommy.template :as template])
  (:require-macros [dommy.template-compile :as template-compile])
  (:use-macros [dommy.template-compile :only [deftemplate]]))


(defn ^:export simple-test []
  (assert (-> :b template/node .-tagName (= "B")))
  (assert (-> "some text" template/node .-textContent (= "some text")))
  ;; unfortunately to satisfy the macro gods, you need to
  ;; duplicate the vector literal to test compiled and runtime template
  (let [e1 (template/node [:span "some text"])
        e2 (template-compile/node [:span "some text"])]
    (doseq [e [e1 e2]]
      (assert (-> e .-tagName (= "SPAN")))
      (assert (-> e .-textContent (= "some text")))
      (assert (-> e .-childNodes (aget 0) .-nodeType (= js/document.TEXT_NODE)))
      (assert (-> e .-children .-length zero?))))
  (let [e1 (template/node [:a {:classes ["class1" "class2"] :href "http://somelink"} "anchor"])
        e2 (template-compile/node
              [:a {:classes ["class1" "class2"] :href "http://somelink"} "anchor"])]
    (doseq [e [e1 e2]] (assert (-> e .-tagName (= "A")))
           (assert (-> e .-textContent (= "anchor")))
           (assert (-> e (.getAttribute "href") (= "http://somelink")))
           (assert (-> e .-className (= "class1 class2")))))
  (let [e1 (template/base-element :div#id.class1.class2)
        e2 (template-compile/node :div#id.class1.class2)]
    (doseq [e [e1 e2]]
      (assert (-> e .-tagName (= "DIV")))
      (assert (-> e (.getAttribute "id") (= "id")))
      (assert (-> e .-className (= "class1 class2")))))
  (let [e1 (template/compound-element [:div {:style {:margin-left "15px"}}])
        e2 (template-compile/node [:div {:style {:margin-left "15px"}}])]
    (doseq [e [e1 e2]]
      (assert (-> e .-tagName (= "DIV")))
      (assert (-> e (.getAttribute "style") (= "margin-left:15px;")))))
  (let [e1 (template/compound-element [:div.class1 [:span#id1 "span1"] [:span#id2 "span2"]])
        e2 (template-compile/node [:div.class1 [:span#id1 "span1"] [:span#id2 "span2"]])]
    (doseq [e [e1 e2]]
      (assert (-> e .-textContent (= "span1span2")))
      (assert (-> e .-className (= "class1")))
      (assert (-> e .-childNodes .-length (= 2)))
      (assert (-> e .-innerHTML (= "<span id=\"id1\">span1</span><span id=\"id2\">span2</span>")))
      (assert (-> e .-childNodes (aget 0) .-innerHTML (= "span1")))
      (assert (-> e .-childNodes (aget 1) .-innerHTML (= "span2")))))
  (assert (= "<span id=\"id1\">span1</span><span id=\"id2\">span2</span>"
             (-> [:div (for [x [1 2]] [:span {:id (str "id" x)} (str "span" x)])]
                 template/node
                 .-innerHTML)))
  (let [e (first (template/html->nodes "<div><p>some-text</p></div>"))]
    (assert (-> e .-tagName (= "DIV")))
    (assert (-> e .-innerHTML (= "<p>some-text</p>")))
    (assert (= e (template/node e))))
  (let [e1 (template/base-element :#id1.class1)
        e2 (template-compile/node :#id1.class1)]
    (doseq [e [e1 e2]]      
      (assert (=  (.-outerHTML (template/base-element :div#id1.class1))
                  (.-outerHTML (template/base-element :#id1.class1))))))
  
  ;; test html for example list form
  ;; note: in practice, if you can write the direct form (without the list) you should.
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end [:span.end "end"]
        h   [:div#id1.class1 (list spans end)]
        e1 (template/compound-element h)
        e2 (template/node             h)]
    (doseq [e [e1 e2]]
      (assert (-> e .-textContent (= "span0span1end")))
      (assert (-> e .-className (= "class1")))
      (assert (-> e .-childNodes .-length (= 3)))
      (assert (-> e .-innerHTML 
                (= "<span>span0</span><span>span1</span><span class=\"end\">end</span>")))
      (assert (-> e .-childNodes (aget 0) .-innerHTML (= "span0")))
      (assert (-> e .-childNodes (aget 1) .-innerHTML (= "span1")))
      (assert (-> e .-childNodes (aget 2) .-innerHTML (= "end")))))
  
  ;; test equivalence of "direct inline" and list forms
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end   [:span.end "end"]
        h1    [:div.class1 (list spans end)]
        h2    [:div.class1 spans end]
        e11 (template/compound-element h1)
        e12 (template/node             h1)
        e21 (template/compound-element h2)
        e22 (template/node             h2)]
    (doseq [[e1 e2] [[e11 e12]
                     [e12 e21]
                     [e21 e22]
                     [e22 e11]]]
      (assert (= (.-innerHTML e1) (.-innerHTML e2)))))
  
  (.log js/console "PASS simple-test"))

(defn ^:export boolean-test []
  (let [e1 (template/node [:option {:selected true} "some text"])
        e1c (template-compile/node [:option {:selected true} "some text"]) 
        e2 (template/node [:option {:selected false} "some text"])
        e2c (template-compile/node [:option {:selected false} "some text"])
        e3 (template/node [:option {:selected nil} "some text"])
        e3c (template-compile/node [:option {:selected nil} "some text"])]
    (doseq [e [e1 e1c]] (assert (-> e (.getAttribute "selected") (= "true"))))
    (doseq [e [e2 e2c]] (assert (-> e (.getAttribute "selected") (nil?))))
    (doseq [e [e3 e3c]] (assert (-> e (.getAttribute "selected") (nil?))))
    (.log js/console "PASS boolean-test")))


(deftemplate simple-template [[href anchor]]
    [:a.anchor {:href href} ^:text anchor])

(defn ^:export deftemplate-test []
  (assert (= "<a class=\"anchor\" href=\"http://somelink.html\">some-text</a>"
         (.-outerHTML (simple-template ["http://somelink.html" "some-text"]))))
  (.log js/console "PASS deftemplate-test"))

(simple-test)
(boolean-test)
(deftemplate-test)
