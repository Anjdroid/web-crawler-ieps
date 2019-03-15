# web-crawler-ieps

-- Assignment 1

zacetne spletne strani:

evem.gov.si
e-uprava.gov.si
podatki.gov.si
e-prostor.gov.si

dodatnih 5:
* stopbirokraciji.gov.si
* mnz.gov.si
* mddsz.gov.si
* mop.gov.si
* mzi.gov.si

TODO:

* HTTP downloader and renderer: To retrieve and render a web page.
	* multiple workers, parallel retrieval
	* # of workers = starting parameter of crawler

* Data extractor: Minimal functionalities to extract images and hyperlinks.
	* link parsing: include links from href & onlick JS events (location.href, document.location)
	* correctly extend relative URLs be4 adding them to frontier
	* detect images on web page (only img tag, where src points to image URL) (A)
	* download other files that pages point to (no need to parse) -> .pdf, .doc, .docx, .ppt, .pptx (A)

* Duplicate detector: To detect already parsed pages.
	* check already parsed urls, check urls in frontier
	* canonicalized URLs only!
	* if no duplicate URL is found: check if a web page with the same content was parsed already
		(extend DB with a hash or compare exact HTML code)

* URL frontier: A list of URLs waiting to be parsed.
	* breadth-first strategy
	** REPORT: explain implementation of strategy
	* robots.txt (user-agent, allow, disallow, crawl-delay)
	* sitemap -> if it exsits, add all urls to frontier

* Datastore: To store the data and additional metadata used by the crawler. (postgresql) (A)
	* create schema using prepared script
	************************************
	Table site contains web site specific data. Each site can contain multiple web pages - 
	table page. Populate all the fields accordingly when parsing. If a page is of type HTML, 
	its content should be stored as a value within html_content attribute, otherwise 
	(if crawler detects a binary file - e.g. .doc), html_content is set to NULL and a record 
	in the page_data table is created. Available page type codes are HTML, BINARY, DUPLICATE 
	and FRONTIER. The duplicate page should not have set the html_content value and should be 
	linked to a duplicate version of a page. You can optionally use table page also as a current 
	frontier queue storage.
	************************************

* libraries for headless browser 
* useful libs:
HTML Cleaner
HTML Parser
JSoup
Jaunt API
HTTP Client
Selenium
phantomJS
HTMLUnit

* forbidden libs:
Scrapy
Apache Nutch
crawler4j
gecco
Norconex HTTP Collector
webmagic
Webmuncher
