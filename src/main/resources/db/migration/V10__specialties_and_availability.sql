-- Medical specialty catalog (bilingual, drives marketplace categories)
CREATE TABLE specialties (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL UNIQUE,
    name_bn     VARCHAR(150)    NOT NULL,
    desc_en     VARCHAR(300)    NOT NULL,
    desc_bn     VARCHAR(400)    NOT NULL,
    icon        VARCHAR(50)     NOT NULL,   -- lucide icon name, mapped in the frontend
    sort_order  INT             NOT NULL DEFAULT 0,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE
);

INSERT INTO specialties (name, name_bn, desc_en, desc_bn, icon, sort_order) VALUES
('Cardiology',              'হৃদরোগ',                'Heart, blood pressure & circulation problems',        'হার্ট, রক্তচাপ ও রক্ত সঞ্চালনের সমস্যা',                    'HeartPulse',     1),
('Dermatology',             'চর্মরোগ',               'Skin, hair & nail conditions',                        'ত্বক, চুল ও নখের সমস্যা',                                   'Hand',           2),
('Endocrinology',           'হরমোন ও ডায়াবেটিস',     'Diabetes, thyroid & hormone disorders',               'ডায়াবেটিস, থাইরয়েড ও হরমোনজনিত রোগ',                       'Droplets',       3),
('Gastroenterology',        'পরিপাকতন্ত্র',           'Stomach, liver & digestion issues',                   'পাকস্থলী, লিভার ও হজমের সমস্যা',                            'Soup',           4),
('General Medicine',        'সাধারণ চিকিৎসা',        'Fever, everyday illness & routine checkups',          'জ্বর, দৈনন্দিন অসুখ ও নিয়মিত স্বাস্থ্য পরীক্ষা',              'Stethoscope',    5),
('General Surgery',         'সার্জারি',              'Surgical consultations & post-operative care',        'অস্ত্রোপচার পরামর্শ ও অস্ত্রোপচার-পরবর্তী যত্ন',              'Scissors',       6),
('Gynecology & Obstetrics', 'স্ত্রীরোগ ও প্রসূতি',     'Women''s health, pregnancy & childbirth',             'নারী স্বাস্থ্য, গর্ভাবস্থা ও প্রসব',                          'HeartHandshake', 7),
('Nephrology',              'কিডনি রোগ',             'Kidney disease & dialysis care',                      'কিডনি রোগ ও ডায়ালাইসিসের চিকিৎসা',                          'FlaskConical',   8),
('Neurology',               'স্নায়ুরোগ',             'Brain, nerves, migraine & stroke care',               'মস্তিষ্ক, স্নায়ু, মাইগ্রেন ও স্ট্রোকের চিকিৎসা',              'Brain',          9),
('Nutrition & Dietetics',   'পুষ্টি',                'Diet plans, weight & lifestyle guidance',             'খাদ্যাভ্যাস, ওজন ও জীবনযাত্রার পরামর্শ',                     'Apple',          10),
('Oncology',                'ক্যান্সার',              'Cancer screening, diagnosis & care',                  'ক্যান্সার পরীক্ষা, নির্ণয় ও চিকিৎসা',                        'Ribbon',         11),
('Ophthalmology',           'চক্ষুরোগ',              'Eyes, vision & cataract care',                        'চোখ, দৃষ্টিশক্তি ও ছানির চিকিৎসা',                           'Eye',            12),
('ENT (Ear, Nose & Throat)','নাক, কান ও গলা',        'Ear, nose, throat & sinus problems',                  'কান, নাক, গলা ও সাইনাসের সমস্যা',                           'Ear',            13),
('Dentistry',               'দন্তরোগ',               'Teeth, gums & oral health',                           'দাঁত, মাড়ি ও মুখের স্বাস্থ্য',                               'Sparkles',       14),
('Orthopedics',             'হাড় ও জয়েন্ট',          'Bones, joints, back pain & injuries',                 'হাড়, জয়েন্ট, কোমর ব্যথা ও আঘাতের চিকিৎসা',                  'Bone',           15),
('Pediatrics',              'শিশুরোগ',               'Child health, growth & vaccinations',                 'শিশুদের স্বাস্থ্য, বৃদ্ধি ও টিকা',                            'Baby',           16),
('Psychiatry',              'মানসিক স্বাস্থ্য',        'Mental health, stress, anxiety & sleep',              'মানসিক স্বাস্থ্য, স্ট্রেস, দুশ্চিন্তা ও ঘুমের সমস্যা',          'Smile',          17),
('Pulmonology',             'ফুসফুস ও শ্বাসতন্ত্র',    'Lungs, asthma & breathing difficulties',              'ফুসফুস, অ্যাজমা ও শ্বাসকষ্টের সমস্যা',                       'Wind',           18),
('Rheumatology',            'বাত ও অটোইমিউন',        'Arthritis, joint pain & autoimmune disease',          'আর্থ্রাইটিস, জয়েন্টের ব্যথা ও অটোইমিউন রোগ',                'Accessibility',  19),
('Urology',                 'মূত্রতন্ত্র',            'Kidney stones, urinary & prostate problems',          'কিডনিতে পাথর, প্রস্রাব ও প্রোস্টেটের সমস্যা',                'Droplet',        20);

-- Doctor-controlled availability: unavailable doctors are hidden from browsing
ALTER TABLE marketplace_doctor_profiles ADD COLUMN is_available BOOLEAN NOT NULL DEFAULT TRUE;
