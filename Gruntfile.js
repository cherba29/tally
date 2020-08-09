module.exports = function (grunt) {
  grunt.loadNpmTasks('grunt-contrib-handlebars');
  grunt.loadNpmTasks('grunt-ts');
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-copy');

  // Project configuration.
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

    handlebars: {
      options: {
        namespace: 'jbudget.templates',
        processName: function (filePath) {
          return filePath.replace(/^templates\//, '').replace(/\.hbs$/, '');
        },
        // Determine if preprocessed template functions will be wrapped in
        // Handlebars.template function.
        wrapped: true
      },
      all: {
        files: {
          "public/templates.js": ["templates/**/*.hbs"]
        }
      }
    },
    copy: {
      handlebars: {
        files: [
          { src: 'js/handlebars.runtime-v4.5.3.js', dest: 'public/' }
        ]
      },
      jquery: {
        files: [
          { src: 'js/jquery-3.5.1.min.js', dest: 'public/' }
        ]
      } 
    },
    clean: {
      out: {
        src: ["public/**/*"]
      }
    },
    ts: {
      default : {
        tsconfig: './tsconfig.json'
      }
    }
  });

  grunt.registerTask('default', ['clean', 'copy', 'handlebars', 'ts']);
};
