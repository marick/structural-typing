# -*- Mode: ruby -*-

require 'rubygems'
require 'rake'

def doit(text)
    puts "== " + text
    system(text)
end

def working_directory_clean?
  output = `git status --porcelain`
  output.empty?
end

desc "Codox into gh-pages branch"
task :doc do
  if working_directory_clean?
    doit("rm -rf /var/tmp/structural-typing-doc")
    doit("lein doc")
    doit("git checkout gh-pages")
    doit("cp -r /var/tmp/structural-typing-doc/* .")
    doit("git add *html")
    doit("git commit -am 'doc update'")
    doit("git push origin gh-pages")
    doit("git checkout master")
  else
    puts "The working directory is not clean"
    doit("git status")
  end
end

desc "Check many versions of Clojure"
task :compatibility do
  doit("lein compatibility")
end
