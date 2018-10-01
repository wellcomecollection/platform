function display_images(query, n='12') {
    document.getElementById('images').innerHTML = '';
    image_div = document.getElementById('images');
    const query_url = '/devise/search?query='.concat(query.toLowerCase()).concat('&n=').concat(n);
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

document.getElementById("query")
    .addEventListener("keypress", function (event) {
        event.preventDefault();
        if (event.key === 'Enter') {
            document.getElementById("search_button").click();
        }
    });