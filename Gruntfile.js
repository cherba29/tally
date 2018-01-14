module.exports = function (grunt) {
  grunt.loadNpmTasks('grunt-contrib-handlebars');
  grunt.loadNpmTasks('grunt-typescript');
  grunt.loadNpmTasks('grunt-ts');

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
          "js/templates.js": ["templates/**/*.hbs"]
        }
      }
    },
    typescript: {
      base: {
        src: ['**/*.ts', "!node_modules/**/*.ts"],
        dest: 'js/',
        options: {
          module: 'amd', //or commonjs 
          target: 'ES6', //or es3 
          sourceMap: true,
          declaration: true
        }
      }
    },
    ts: {
      options: {
        target: "ES6",
      },
      default : {
          src: ["**/*.ts", "!node_modules/**/*.ts"]
      }
    }
  });

  grunt.registerTask('default', ['handlebars', 'ts']);
};
