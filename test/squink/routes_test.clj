(ns squink.routes-test
  (require [squink.routes :refer [handler]]
           [squink.core :refer [shorten *db*]]
           [squink.mysql :refer [lookup-url]]
           [midje.sweet :refer :all]))

(facts "GET /"
       (fact "when hash is found, returns 301 and url in Location header"
             (against-background (lookup-url *db* "HaSh") => "some-url")
             (let [request {:request-method :get :uri "/HaSh"}]
               (handler request) => (contains {:status 301})
               (handler request) => (contains {:headers {"Location" "some-url"}})))

       (fact "when hash is not found, returns 404 status"
             (handler {:request-method :get :uri "/sdfd"}) => (contains {:status 404})
             (provided (lookup-url *db* "sdfd") => nil)))

(let [request {:request-method :post :uri "/" :params {:url "test.com"}}]
  (facts "POST /"
         (fact "when the url is invalid return 400 and invalid url in body"
               (handler (assoc request :params {:url "halllooo"})) =>
               (contains {:status 400 :body "invalid url"}))

         (fact "when no url is specified return 400"
               (handler (assoc request :params {})) =>
               (contains {:status 400 :body "invalid url"}))

         (fact "when the hash for the url cannot be calcuated"
               (handler request) =>
               (contains {:status 400 :body "uncomputable hash!"})
               (provided (shorten "http://test.com/") => nil))

         (fact "when the hash is calculated successfully return 201 and hash"
               (handler request) =>
               (contains {:status 201 :body ..hashed..})
               (provided (shorten "http://test.com/") => ..hashed..))))