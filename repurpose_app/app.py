from flask import Flask, request, jsonify, render_template
import requests
from bs4 import BeautifulSoup
import re

app = Flask(__name__)

def get_hashtags(text, num_hashtags=5):
    words = re.findall(r'\b\w+\b', text.lower())
    # A simple stopword list, can be expanded
    stopwords = set(['the', 'a', 'an', 'in', 'is', 'it', 'and', 'of', 'to', 'for', 'on', 'with', 'as', 'by', 'that', 'this'])
    words = [word for word in words if word not in stopwords and len(word) > 3]
    # Get the most common words
    from collections import Counter
    most_common_words = [word for word, count in Counter(words).most_common(num_hashtags)]
    return ' '.join([f"#{word.capitalize()}" for word in most_common_words])

def generate_linkedin_post(text):
    summary = ' '.join(text.split()[:150]) + '...'
    hashtags = get_hashtags(text)
    post = f"""**A Thought-Provoking Insight**

{summary}

What are your thoughts on this? Let's discuss in the comments!

{hashtags}"""
    return post

def generate_twitter_thread(text, url):
    # Find a sentence with a number for a potential hook
    sentences = text.split('.')
    hook_sentence = sentences[0] # Default to the first sentence
    for sentence in sentences:
        if any(char.isdigit() for char in sentence):
            hook_sentence = sentence
            break

    hook = " ".join(hook_sentence.split()[:20]) + "..."
    if len(hook) > 140:
        hook = hook[:140]

    takeaway_1 = " ".join(text.split()[15:45]) + "..."
    takeaway_2 = " ".join(text.split()[45:75]) + "..."

    thread = [
        f"🔥 {hook}",
        f"Key takeaway: {takeaway_1}",
        f"Another point: {takeaway_2}\n\nRead the full analysis here: {url}"
    ]
    return thread

def generate_instagram_caption(text):
    caption = " ".join(text.split()[:20]) + "..."
    hashtags = get_hashtags(text)
    return f"{caption}\n\n{hashtags}"

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/repurpose', methods=['POST'])
def repurpose():
    data = request.get_json()
    url = data.get('url')

    if not url:
        return jsonify({'error': 'URL is required'}), 400

    try:
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        response = requests.get(url, headers=headers)
        response.raise_for_status()
        soup = BeautifulSoup(response.content, 'html.parser')

        # Prioritize finding the main content of the article
        if soup.article:
            main_content = soup.article
        elif soup.find(id='content'):
            main_content = soup.find(id='content')
        elif soup.find(id='main'):
            main_content = soup.find(id='main')
        else:
            main_content = soup

        # Remove script, style, nav, header, and footer elements from the main content
        for unwanted_tag in main_content(['script', 'style', 'nav', 'header', 'footer']):
            unwanted_tag.decompose()

        # Get text and clean it up
        text = main_content.get_text()
        lines = (line.strip() for line in text.splitlines())
        chunks = (phrase.strip() for line in lines for phrase in line.split("  "))
        cleaned_text = '\n'.join(chunk for chunk in chunks if chunk)

        if not cleaned_text:
            return jsonify({'error': 'Could not extract text from the URL.'}), 400

        linkedin_post = generate_linkedin_post(cleaned_text)
        twitter_thread = generate_twitter_thread(cleaned_text, url)
        instagram_caption = generate_instagram_caption(cleaned_text)

        reel_idea = "Text overlay on a split screen graphic showing a key statistic vs. its impact."

        return jsonify({
            'linkedin': linkedin_post,
            'twitter': twitter_thread,
            'instagram': {
                'caption': instagram_caption,
                'reel_idea': reel_idea
            }
        })

    except requests.exceptions.RequestException as e:
        app.logger.error('Failed to fetch URL', exc_info=True)
        return jsonify({'error': f'Failed to fetch URL: {e}'}), 500
    except Exception as e:
        app.logger.error('An error occurred', exc_info=True)
        return jsonify({'error': f'An error occurred: {e}'}), 500

if __name__ == '__main__':
    app.run(debug=True, port=5001)
