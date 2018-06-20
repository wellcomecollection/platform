function display_most_similar_images(query_id) {
    document.getElementById('images').innerHTML = '';
    image_div = document.getElementById('images');
    const query_url = 'http://127.0.0.1:5000/api/most_similar/'.concat(query_id).concat('.jpg');
    fetch(query_url)
        .then((resp) => resp.json())
        .then(function (data) {
            let image_urls = data.response;
            return image_urls.map(function (image_url) {
                outer_div = document.createElement('div')
                outer_div.className = 'fl w-100 w-50-m w-25-ns pa3-ns';

                inner_div = document.createElement('div')
                inner_div.className = 'aspect-ratio aspect-ratio--1x1';

                img = document.createElement('img')
                img.src = image_url;
                img.className = 'db bg-center cover aspect-ratio--object';

                inner_div.appendChild(img);
                outer_div.appendChild(inner_div);
                image_div.appendChild(outer_div);
            })
        })
        .catch(function (error) {
            console.log(error);
        });
}


function display_palette_search_images(the_form) {
    document.getElementById('images').innerHTML = '';

    const c_1 = document.getElementById('form').c_1.value;
    const c_2 = document.getElementById('form').c_2.value;
    const c_3 = document.getElementById('form').c_3.value;
    const c_4 = document.getElementById('form').c_4.value;
    const c_5 = document.getElementById('form').c_5.value;
    const params = c_1.concat(c_2).concat(c_3).concat(c_4).concat(c_5)
    const query_url = 'http://127.0.0.1:5000/api/palette_search/'.concat(params);
    console.log(query_url)
    
    image_div = document.getElementById('images');
    fetch(query_url)
        .then((resp) => resp.json())
        .then(function (data) {
            let image_urls = data.response;
            return image_urls.map(function (image_url) {
                outer_div = document.createElement('div')
                outer_div.className = 'fl w-100 w-50-m w-25-ns pa3-ns';

                inner_div = document.createElement('div')
                inner_div.className = 'aspect-ratio aspect-ratio--1x1';

                img = document.createElement('img')
                img.src = image_url;
                img.className = 'db bg-center cover aspect-ratio--object';

                inner_div.appendChild(img);
                outer_div.appendChild(inner_div);
                image_div.appendChild(outer_div);
            })
        })
        .catch(function (error) {
            console.log(error);
        });
}