require 'edn'

def project_version
  project = File.open 'project.clj' do |f|
    EDN.read(f)
  end
  project[2]
end

desc "Run the code formatter"
task :format do
  sh 'lein cljfmt fix project.clj src/telemetry/*.clj'
end

desc "Print the current project version"
task :version do
  puts project_version
end

desc "Run Riemann with the local standalone jar"
task :riemann do
  sh "EXTRA_CLASSPATH=#{File.dirname(__FILE__)}/target/telemetry-#{project_version}-standalone.jar riemann ./riemann.config.example"
end
